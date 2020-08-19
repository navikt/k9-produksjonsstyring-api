package no.nav.k9.tjenester.saksbehandler.oppgave

import info.debatty.java.stringsimilarity.Levenshtein
import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.Configuration
import no.nav.k9.KoinProfile
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.lager.oppgave.Reservasjon
import no.nav.k9.domene.modell.OppgaveKø
import no.nav.k9.domene.modell.Saksbehandler
import no.nav.k9.domene.repository.*
import no.nav.k9.integrasjon.abac.IPepClient
import no.nav.k9.integrasjon.azuregraph.IAzureGraphService
import no.nav.k9.integrasjon.pdl.AktøridPdl
import no.nav.k9.integrasjon.pdl.PdlService
import no.nav.k9.integrasjon.pdl.navn
import no.nav.k9.integrasjon.rest.idToken
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleOppgaverBeholdningHistorikk
import no.nav.k9.tjenester.fagsak.FagsakDto
import no.nav.k9.tjenester.fagsak.PersonDto
import no.nav.k9.tjenester.mock.Aksjonspunkter
import no.nav.k9.tjenester.saksbehandler.nokkeltall.NyeOgFerdigstilteOppgaverDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import kotlin.coroutines.coroutineContext
import kotlin.streams.toList
import kotlin.system.measureTimeMillis

private val log: Logger =
    LoggerFactory.getLogger(OppgaveTjeneste::class.java)

class OppgaveTjeneste @KtorExperimentalAPI constructor(
    private val oppgaveRepository: OppgaveRepository,
    private val oppgaveKøRepository: OppgaveKøRepository,
    private val saksbehandlerRepository: SaksbehandlerRepository,
    private val pdlService: PdlService,
    private val reservasjonRepository: ReservasjonRepository,
    private val configuration: Configuration,
    private val azureGraphService: IAzureGraphService,
    private val pepClient: IPepClient,
    private val statistikkRepository: StatistikkRepository
) {

    fun hentOppgaver(oppgavekøId: UUID): List<Oppgave> {
        return try {
            val oppgaveKø = oppgaveKøRepository.hentOppgavekø(oppgavekøId)
            oppgaveRepository.hentOppgaver(oppgaveKø.oppgaverOgDatoer.take(20).map { it.id })
        } catch (e: Exception) {
            log.error("Henting av oppgave feilet, returnerer en tom oppgaveliste", e)
            emptyList()
        }
    }

    @KtorExperimentalAPI
    suspend fun reserverOppgave(ident: String, uuid: UUID): OppgaveStatusDto {
        if (!pepClient.harTilgangTilReservingAvOppgaver()) {
            return OppgaveStatusDto(
                erReservert = false,
                reservertTilTidspunkt = null,
                erReservertAvInnloggetBruker = false,
                reservertAv = null,
                flyttetReservasjon = null
            )
        }
        val reservasjon = Reservasjon(
            reservertTil = LocalDateTime.now().plusHours(24).forskyvReservasjonsDato(),
            reservertAv = ident, flyttetAv = null, flyttetTidspunkt = null, begrunnelse = null, oppgave = uuid
        )

        try {
            reservasjonRepository.lagre(uuid, true) {
                if (it != null && it.erAktiv()) {
                    val oppgave = oppgaveRepository.hent(uuid)
                    throw IllegalArgumentException("Oppgaven er allerede reservert $uuid ${oppgave.fagsakSaksnummer}, $ident prøvde å reservere saken")
                }
                saksbehandlerRepository.leggTilReservasjon(reservasjon.reservertAv, reservasjon.oppgave)
                reservasjon
            }
            val oppgave = oppgaveRepository.hent(uuid)
            for (oppgaveKø in oppgaveKøRepository.hent()) {
                oppgaveKøRepository.lagre(oppgaveKø.id, refresh = true) {
                    it!!.leggOppgaveTilEllerFjernFraKø(oppgave, reservasjonRepository)
                    it
                }
            }
            return OppgaveStatusDto(
                erReservert = true,
                reservertTilTidspunkt = reservasjon.reservertTil,
                erReservertAvInnloggetBruker = reservertAvMeg(ident),
                reservertAv = ident,
                flyttetReservasjon = null
            )
        } catch (e: java.lang.IllegalArgumentException) {
            log.warn(e.message)
            return OppgaveStatusDto(
                erReservert = true,
                reservertTilTidspunkt = reservasjon.reservertTil,
                erReservertAvInnloggetBruker = false,
                reservertAv = reservasjon.reservertAv,
                flyttetReservasjon = null
            )
        }
    }

    @KtorExperimentalAPI
    suspend fun søkFagsaker(query: String): List<FagsakDto> {
        if (query.length == 11) {
            var aktørId = pdlService.identifikator(query)
            if (!(configuration.koinProfile() == KoinProfile.PROD)) {
                aktørId = AktøridPdl(
                    data = AktøridPdl.Data(
                        hentIdenter = AktøridPdl.Data.HentIdenter(
                            identer = listOf(
                                AktøridPdl.Data.HentIdenter.Identer(
                                    gruppe = "AKTORID",
                                    historisk = false,
                                    ident = "2392173967319"
                                )
                            )
                        )
                    )
                )
            }
            if (aktørId != null && aktørId.data.hentIdenter != null && aktørId.data.hentIdenter!!.identer.isNotEmpty()) {
                var aktorId = aktørId.data.hentIdenter!!.identer[0].ident
                val person = pdlService.person(aktorId)
                if (person != null) {
                    if (!(configuration.koinProfile() == KoinProfile.PROD)) {
                        aktorId = "1172507325105"
                    }
                    return oppgaveRepository.hentOppgaverMedAktorId(aktorId).filter {
                        if (!pepClient.harTilgangTilLesSak(
                                fagsakNummer = it.fagsakSaksnummer,
                                aktørid = it.aktorId
                            )
                        ) {
                            settSkjermet(it)
                            false
                        } else {
                            true
                        }
                    }.map {
                        FagsakDto(
                            it.fagsakSaksnummer,
                            PersonDto(
                                person.navn(),
                                person.data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer,
                                person.data.hentPerson.kjoenn[0].kjoenn,
                                null
                                //   person.data.hentPerson.doedsfall[0].doedsdato
                            ),
                            it.fagsakYtelseType,
                            it.behandlingStatus,
                            it.behandlingOpprettet,
                            it.aktiv
                        )
                    }
                }
            }
        }
        val oppgaver = oppgaveRepository.hentOppgaverMedSaksnummer(query)
        val ret = mutableListOf<FagsakDto>()
        for (oppgave in oppgaver) {
            if (!pepClient.harTilgangTilLesSak(
                    fagsakNummer = oppgave.fagsakSaksnummer,
                    aktørid = oppgave.aktorId
                )
            ) {
                settSkjermet(oppgave)
                return emptyList()
            }
            val person = pdlService.person(oppgave.aktorId)!!

            ret.add(
                FagsakDto(
                    oppgave.fagsakSaksnummer,
                    PersonDto(
                        person.navn(),
                        person.data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer,
                        person.data.hentPerson.kjoenn[0].kjoenn,
                        null
                        // person.data.hentPerson.doedsfall!!.doedsdato
                    ),
                    oppgave.fagsakYtelseType,
                    oppgave.behandlingStatus,
                    oppgave.behandlingOpprettet,
                    oppgave.aktiv
                )
            )
        }
        return ret
    }

    @KtorExperimentalAPI
    suspend fun reservertAvMeg(ident: String?): Boolean {
        return azureGraphService.hentIdentTilInnloggetBruker() == ident
    }

    @KtorExperimentalAPI
    suspend fun tilOppgaveDto(oppgave: Oppgave, reservasjon: Reservasjon?): OppgaveDto {

        val oppgaveStatus =
            if (reservasjon != null && (!reservasjon.erAktiv()) || reservasjon == null) {
                OppgaveStatusDto(false, null, false, null, null)
            } else {
                OppgaveStatusDto(
                    erReservert = true,
                    reservertTilTidspunkt = reservasjon.reservertTil,
                    erReservertAvInnloggetBruker = reservertAvMeg(reservasjon.reservertAv),
                    reservertAv = reservasjon.reservertAv,
                    flyttetReservasjon = null
                )
            }
        val person = pdlService.person(oppgave.aktorId)!!
        return OppgaveDto(
            status = oppgaveStatus,
            behandlingId = oppgave.behandlingId,
            saksnummer = oppgave.fagsakSaksnummer,
            navn = person.navn(),
            system = oppgave.system,
            personnummer = person.data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer,
            behandlingstype = oppgave.behandlingType,
            fagsakYtelseType = oppgave.fagsakYtelseType,
            behandlingStatus = oppgave.behandlingStatus,
            erTilSaksbehandling = oppgave.aktiv,
            opprettetTidspunkt = oppgave.behandlingOpprettet,
            behandlingsfrist = oppgave.behandlingsfrist,
            eksternId = oppgave.eksternId,
            tilBeslutter = oppgave.tilBeslutter,
            utbetalingTilBruker = oppgave.utbetalingTilBruker,
            selvstendigFrilans = oppgave.selvstendigFrilans,
            kombinert = oppgave.kombinert,
            søktGradering = oppgave.søktGradering,
            registrerPapir = oppgave.registrerPapir
        )
    }


    @KtorExperimentalAPI
    suspend fun hentOppgaverFraListe(saksnummere: List<String>): List<OppgaveDto> {
        return saksnummere.flatMap { oppgaveRepository.hentOppgaverMedSaksnummer(it) }
            .map { oppgave ->
                tilOppgaveDto(
                    oppgave = oppgave, reservasjon = if (reservasjonRepository.finnes(oppgave.eksternId)
                        && reservasjonRepository.hent(oppgave.eksternId).erAktiv()
                    ) {
                        reservasjonRepository.hent(oppgave.eksternId)
                    } else {
                        null
                    }
                )
            }.toList()
    }

    fun hentNyeOgFerdigstilteOppgaver(oppgavekoId: String): List<NyeOgFerdigstilteOppgaverDto> {
        return oppgaveKøRepository.hentOppgavekø(UUID.fromString(oppgavekoId))
            .nyeOgFerdigstilteOppgaverPerAntallDager(7)
            .map {
                NyeOgFerdigstilteOppgaverDto(
                    behandlingType = it.behandlingType,
                    dato = it.dato,
                    antallNye = it.nye.size,
                    antallFerdigstilte = it.ferdigstilte.size
                )
            }
    }

    @KtorExperimentalAPI
    suspend fun hentNyeOgFerdigstilteOppgaver(): List<NyeOgFerdigstilteOppgaverDto> {
        return statistikkRepository.hentFerdigstilteOgNyeHistorikkPerAntallDager(7).map {
            val hentIdentTilInnloggetBruker = if (KoinProfile.LOCAL == configuration.koinProfile()) {
                "saksbehandler@nav.no"
            } else {
                azureGraphService.hentIdentTilInnloggetBruker()
            }
            val antallFerdistilteMine =
                reservasjonRepository.hentSelvOmDeIkkeErAktive(it.ferdigstilte.map { UUID.fromString(it)!! }
                    .toSet())
                    .filter { it.reservertAv == hentIdentTilInnloggetBruker }.size
            NyeOgFerdigstilteOppgaverDto(
                behandlingType = it.behandlingType,
                dato = it.dato,
                antallNye = it.nye,
                antallFerdigstilte = it.ferdigstilte.size,
                antallFerdigstilteMine = antallFerdistilteMine
            )
        }
    }

    fun hentBeholdningAvOppgaverPerAntallDager(): List<AlleOppgaverBeholdningHistorikk> {
        var antalletTotalt = oppgaveRepository.hentAktiveOppgaverTotalt()
        return statistikkRepository.hentFerdigstilteOgNyeHistorikkPerAntallDager(28).map {
            antalletTotalt = antalletTotalt - it.nye + it.ferdigstilte.size
            AlleOppgaverBeholdningHistorikk(
                it.fagsakYtelseType,
                it.behandlingType,
                it.dato,
                antalletTotalt
            )
        }
    }

    suspend fun frigiReservasjon(uuid: UUID, begrunnelse: String): Reservasjon {
        val reservasjon = reservasjonRepository.lagre(uuid, true) {
            it!!.begrunnelse = begrunnelse
            saksbehandlerRepository.fjernReservasjon(it.reservertAv, it.oppgave)
            it.reservertTil = null
            it
        }
        val oppgave = oppgaveRepository.hent(uuid)
        for (oppgaveKø in oppgaveKøRepository.hent()) {
            oppgaveKøRepository.lagre(oppgaveKø.id, refresh = true) {
                it!!.leggOppgaveTilEllerFjernFraKø(oppgave, reservasjonRepository)
                it
            }
        }
        return reservasjon
    }

    fun forlengReservasjonPåOppgave(uuid: UUID): Reservasjon {
        return reservasjonRepository.lagre(uuid, true) {
            it!!.reservertTil = it.reservertTil?.plusHours(24)!!.forskyvReservasjonsDato()
            it
        }
    }

    fun endreReservasjonPåOppgave(resEndring: ReservasjonEndringDto): Reservasjon {
        return reservasjonRepository.lagre(UUID.fromString(resEndring.oppgaveId), true) {
            it!!.reservertTil = LocalDateTime.of(
                resEndring.reserverTil.year,
                resEndring.reserverTil.month,
                resEndring.reserverTil.dayOfMonth,
                23,
                59,
                59
            ).forskyvReservasjonsDato()
            it
        }
    }

    suspend fun flyttReservasjon(uuid: UUID, ident: String, begrunnelse: String): Reservasjon {
        if (ident == "") {
            return reservasjonRepository.hent(uuid)
        }
        val hentIdentTilInnloggetBruker = azureGraphService.hentIdentTilInnloggetBruker()
        return reservasjonRepository.lagre(uuid, true) {
            it!!.reservertTil = it.reservertTil?.plusHours(24)!!.forskyvReservasjonsDato()
            it.flyttetTidspunkt = LocalDateTime.now()
            saksbehandlerRepository.fjernReservasjon(it.reservertAv, it.oppgave)
            it.reservertAv = ident
            it.flyttetAv = hentIdentTilInnloggetBruker
            saksbehandlerRepository.leggTilReservasjon(ident, it.oppgave)
            it.begrunnelse = begrunnelse
            it
        }
    }

    suspend fun hentSisteBehandledeOppgaver(): List<BehandletOppgave> {
        return statistikkRepository.hentBehandlinger(coroutineContext.idToken().getUsername())
    }

    fun flyttReservasjonTilForrigeSakbehandler(uuid: UUID) {
        val reservasjoner = reservasjonRepository.hentMedHistorikk(uuid).reversed()
        for (reservasjon in reservasjoner) {
            if (reservasjoner[0].reservertAv != reservasjon.reservertAv) {
                reservasjonRepository.lagre(uuid, true) {
                    saksbehandlerRepository.fjernReservasjon(it!!.reservertAv, reservasjon.oppgave)
                    it.reservertAv = reservasjon.reservertAv
                    saksbehandlerRepository.leggTilReservasjon(reservasjon.reservertAv, reservasjon.oppgave)
                    it.reservertTil = LocalDateTime.now().plusDays(3).forskyvReservasjonsDato()
                    it
                }
                return
            }
        }
    }

    fun hentReservasjonsHistorikk(uuid: UUID): ReservasjonHistorikkDto {
        val reservasjoner = reservasjonRepository.hentMedHistorikk(uuid).reversed()
        return ReservasjonHistorikkDto(
            reservasjoner = reservasjoner.map {
                ReservasjonDto(
                    reservertTil = it.reservertTil,
                    reservertAv = it.reservertAv,
                    flyttetAv = it.flyttetAv,
                    flyttetTidspunkt = it.flyttetTidspunkt,
                    begrunnelse = it.begrunnelse
                )
            }.toList(),
            oppgaveId = uuid.toString()
        )
    }

    suspend fun hentAntallOppgaver(oppgavekøId: UUID, taMedReserverte: Boolean = false): Int {
        val oppgavekø = oppgaveKøRepository.hentOppgavekø(oppgavekøId)
        var reserverteOppgaverSomHørerTilKø = 0
        if (taMedReserverte) {
            val reservasjoner = reservasjonRepository.hent(
                saksbehandlerRepository.hentAlleSaksbehandlere()
                    .flatMap { saksbehandler -> saksbehandler.reservasjoner }.toSet()
            )

            for (oppgave in oppgaveRepository.hentOppgaver(reservasjoner.map { it.oppgave })) {
                if (oppgavekø.tilhørerOppgaveTilKø(oppgave, reservasjonRepository, false)) {
                    reserverteOppgaverSomHørerTilKø++
                }
            }
        }
        return oppgavekø.oppgaverOgDatoer.size + reserverteOppgaverSomHørerTilKø
    }

    fun hentAntallOppgaverTotalt(): Int {
        return oppgaveRepository.hentAktiveOppgaverTotalt()
    }

    @KtorExperimentalAPI
    suspend fun hentNesteOppgaverIKø(kø: UUID): List<OppgaveDto> {
        if (configuration.koinProfile() != KoinProfile.LOCAL) {
            if (pepClient.harBasisTilgang()) {
                val list = mutableListOf<OppgaveDto>()
                val ms = measureTimeMillis {
                    for (oppgave in hentOppgaver(kø)) {
                        if (list.size == 10) {
                            break
                        }
                        if (!pepClient.harTilgangTilLesSak(
                                fagsakNummer = oppgave.fagsakSaksnummer,
                                aktørid = oppgave.aktorId
                            )
                        ) {
                            settSkjermet(oppgave)
                            continue
                        }
                        val person = pdlService.person(oppgave.aktorId)

                        val navn = if (KoinProfile.PREPROD == configuration.koinProfile()) {
                            "${oppgave.fagsakSaksnummer} " +
                                    oppgave.aksjonspunkter.liste.entries.stream().map { t ->
                                        val a = Aksjonspunkter().aksjonspunkter()
                                            .find { aksjonspunkt -> aksjonspunkt.kode == t.key }
                                        "${t.key} ${a?.navn ?: "Ukjent aksjonspunkt"}"
                                    }.toList().joinToString(", ")
                        } else {
                            person?.navn() ?: "Uten navn"
                        }

                        list.add(
                            OppgaveDto(
                                status = OppgaveStatusDto(
                                    erReservert = false,
                                    reservertTilTidspunkt = null,
                                    erReservertAvInnloggetBruker = false,
                                    reservertAv = null,
                                    flyttetReservasjon = null
                                ),
                                behandlingId = oppgave.behandlingId,
                                saksnummer = oppgave.fagsakSaksnummer,
                                navn = navn,
                                system = oppgave.system,
                                personnummer = if (person == null) {
                                    "Ukjent fnummer"
                                } else {
                                    person.data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer
                                },
                                behandlingstype = oppgave.behandlingType,
                                fagsakYtelseType = oppgave.fagsakYtelseType,
                                behandlingStatus = oppgave.behandlingStatus,
                                erTilSaksbehandling = oppgave.aktiv,
                                opprettetTidspunkt = oppgave.behandlingOpprettet,
                                behandlingsfrist = oppgave.behandlingsfrist,
                                eksternId = oppgave.eksternId,
                                tilBeslutter = oppgave.tilBeslutter,
                                utbetalingTilBruker = oppgave.utbetalingTilBruker,
                                søktGradering = oppgave.søktGradering,
                                selvstendigFrilans = oppgave.selvstendigFrilans,
                                registrerPapir = oppgave.registrerPapir,
                                kombinert = oppgave.kombinert
                            )
                        )
                    }
                }
                log.info("Hentet ${list.size} oppgaver for oppgaveliste tok $ms")
                return list
            } else {
                log.warn("har ikke basistilgang")
                return emptyList()
            }
        } else {
            val list = mutableListOf<OppgaveDto>()
            val oppgaver = hentOppgaver(kø)
            for (oppgave in oppgaver) {
                list.add(
                    OppgaveDto(
                        status = OppgaveStatusDto(
                            erReservert = false,
                            reservertTilTidspunkt = null,
                            erReservertAvInnloggetBruker = false,
                            reservertAv = null,
                            flyttetReservasjon = null
                        ),
                        behandlingId = oppgave.behandlingId,
                        saksnummer = oppgave.fagsakSaksnummer,
                        navn = "Navn",
                        system = oppgave.system,
                        personnummer = oppgave.aktorId,
                        behandlingstype = oppgave.behandlingType,
                        fagsakYtelseType = oppgave.fagsakYtelseType,
                        behandlingStatus = oppgave.behandlingStatus,
                        erTilSaksbehandling = oppgave.aktiv,
                        opprettetTidspunkt = oppgave.behandlingOpprettet,
                        behandlingsfrist = oppgave.behandlingsfrist,
                        eksternId = oppgave.eksternId,
                        tilBeslutter = oppgave.tilBeslutter,
                        utbetalingTilBruker = oppgave.utbetalingTilBruker,
                        søktGradering = oppgave.søktGradering,
                        selvstendigFrilans = oppgave.selvstendigFrilans,
                        registrerPapir = oppgave.registrerPapir,
                        kombinert = oppgave.kombinert
                    )
                )
            }
            return list
        }
    }

    @KtorExperimentalAPI
    suspend fun hentSisteReserverteOppgaver(): List<OppgaveDto> {
        val list = mutableListOf<OppgaveDto>()
        //Hent reservasjoner for en gitt bruker skriv om til å hente med ident direkte i tabellen
        val saksbehandlerMedEpost =
            saksbehandlerRepository.finnSaksbehandlerMedEpost(coroutineContext.idToken().getUsername())
        val brukerIdent = saksbehandlerMedEpost?.brukerIdent ?: return emptyList()
        val reservasjoner = reservasjonRepository.hent(brukerIdent)
        for (reservasjon in reservasjoner
            .sortedBy { reservasjon -> reservasjon.reservertTil }) {
            val oppgave = oppgaveRepository.hent(reservasjon.oppgave)
            if (!tilgangTilSak(oppgave)) continue

            val person = pdlService.person(oppgave.aktorId)

            val status =
                OppgaveStatusDto(
                    true,
                    reservasjon.reservertTil,
                    true,
                    reservasjon.reservertAv,
                    flyttetReservasjon = if (reservasjon.flyttetAv.isNullOrEmpty()) {
                        null
                    } else {
                        FlyttetReservasjonDto(
                            reservasjon.flyttetTidspunkt!!,
                            reservasjon.flyttetAv!!,
                            saksbehandlerRepository.finnSaksbehandlerMedIdent(reservasjon.flyttetAv!!)?.navn!!,
                            reservasjon.begrunnelse!!
                        )
                    }
                )
            var personNavn: String
            var personFnummer: String
            val navn = if (KoinProfile.PREPROD == configuration.koinProfile()) {
                "${oppgave.fagsakSaksnummer} " + 
                    oppgave.aksjonspunkter.liste.entries.stream().map { t ->
                        val a = Aksjonspunkter().aksjonspunkter()
                            .find { aksjonspunkt -> aksjonspunkt.kode == t.key }
                        "${t.key} ${a?.navn ?: "Ukjent aksjonspunkt"}"
                    }.toList().joinToString(", ")
            } else {
                person?.navn() ?: "Uten navn"
            }
            personNavn = navn
            personFnummer = if (person == null) {
                "Ukent fnummer"
            } else {
                person.data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer
            }
            list.add(
                OppgaveDto(
                    status = status,
                    behandlingId = oppgave.behandlingId,
                    saksnummer = oppgave.fagsakSaksnummer,
                    navn = personNavn,
                    system = oppgave.system,
                    personnummer = personFnummer,
                    behandlingstype = oppgave.behandlingType,
                    fagsakYtelseType = oppgave.fagsakYtelseType,
                    behandlingStatus = oppgave.behandlingStatus,
                    erTilSaksbehandling = true,
                    opprettetTidspunkt = oppgave.behandlingOpprettet,
                    behandlingsfrist = oppgave.behandlingsfrist,
                    eksternId = oppgave.eksternId,
                    tilBeslutter = oppgave.tilBeslutter,
                    utbetalingTilBruker = oppgave.utbetalingTilBruker,
                    selvstendigFrilans = oppgave.selvstendigFrilans,
                    kombinert = oppgave.kombinert,
                    søktGradering = oppgave.søktGradering,
                    registrerPapir = oppgave.registrerPapir
                )
            )
        }
        return list
    }

    suspend fun tilgangTilSak(oppgave: Oppgave): Boolean {
        if (!pepClient.harTilgangTilLesSak(
                fagsakNummer = oppgave.fagsakSaksnummer,
                aktørid = oppgave.aktorId
            )
        ) {
            reservasjonRepository.lagre(oppgave.eksternId, true) {
                it!!.reservertTil = null
                saksbehandlerRepository.fjernReservasjon(it.reservertAv, it.oppgave)
                it
            }
            settSkjermet(oppgave)
            oppgaveKøRepository.hent().forEach { oppgaveKø ->
                oppgaveKøRepository.lagre(oppgaveKø.id) {
                    it!!.leggOppgaveTilEllerFjernFraKø(oppgave, reservasjonRepository)
                    it
                }
            }
            return false
        }
        return true
    }

    fun sokSaksbehandlerMedIdent(ident: BrukerIdentDto): Saksbehandler? {
        return saksbehandlerRepository.finnSaksbehandlerMedIdent(ident.brukerIdent)
    }

    fun sokSaksbehandler(søkestreng: String): Saksbehandler? {
        val alleSaksbehandlere = saksbehandlerRepository.hentAlleSaksbehandlere()
        val levenshtein = Levenshtein()

        var d = Double.MAX_VALUE
        var i = -1
        for ((index, saksbehandler) in alleSaksbehandlere.withIndex()) {
            if (saksbehandler.brukerIdent == null) {
                continue
            }
            if (saksbehandler.navn != null && saksbehandler.navn!!.toLowerCase().contains(søkestreng, true)) {
                i = index
                break
            }

            var distance = levenshtein.distance(søkestreng.toLowerCase(), saksbehandler.brukerIdent!!.toLowerCase())
            if (distance < d) {
                d = distance
                i = index
            }
            distance = levenshtein.distance(søkestreng.toLowerCase(), saksbehandler.navn!!.toLowerCase())
            if (distance < d) {
                d = distance
                i = index
            }
            distance = levenshtein.distance(søkestreng.toLowerCase(), saksbehandler.epost.toLowerCase())
            if (distance < d) {
                d = distance
                i = index
            }
        }
        return alleSaksbehandlere[i]
    }

    fun hentOppgaveKøer(): List<OppgaveKø> {
        return oppgaveKøRepository.hent()
    }

    @KtorExperimentalAPI
    fun leggTilBehandletOppgave(ident: String, oppgave: BehandletOppgave) {
        return statistikkRepository.lagreBehandling(ident) {
            oppgave
        }
    }

    suspend fun settSkjermet(oppgave: Oppgave) {
        log.info("Skjermer oppgave")
        oppgave.skjermet = true
        oppgaveRepository.lagre(oppgave.eksternId) { it ->
            it?.skjermet = true
            it!!
        }
        val oppgaKøer = oppgaveKøRepository.hent()
        for (oppgaveKø in oppgaKøer) {
            val skalOppdareKø = oppgaveKø.leggOppgaveTilEllerFjernFraKø(oppgave, reservasjonRepository)
            if (skalOppdareKø) {
                oppgaveKøRepository.lagre(oppgaveKø.id) {
                    it!!.leggOppgaveTilEllerFjernFraKø(oppgave, reservasjonRepository)
                    it
                }
            }
        }
    }
}
