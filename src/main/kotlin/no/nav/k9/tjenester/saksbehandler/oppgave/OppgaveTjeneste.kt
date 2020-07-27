package no.nav.k9.tjenester.saksbehandler.oppgave

import io.ktor.util.KtorExperimentalAPI
import joptsimple.internal.Strings
import no.nav.k9.Configuration
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.lager.oppgave.Reservasjon
import no.nav.k9.domene.modell.OppgaveKø
import no.nav.k9.domene.modell.Saksbehandler
import no.nav.k9.domene.repository.*
import no.nav.k9.integrasjon.abac.PepClient
import no.nav.k9.integrasjon.azuregraph.AzureGraphService
import no.nav.k9.integrasjon.pdl.AktøridPdl
import no.nav.k9.integrasjon.pdl.PdlService
import no.nav.k9.integrasjon.pdl.navn
import no.nav.k9.tjenester.fagsak.FagsakDto
import no.nav.k9.tjenester.fagsak.PersonDto
import no.nav.k9.tjenester.mock.Aksjonspunkter
import no.nav.k9.tjenester.saksbehandler.nokkeltall.NyeOgFerdigstilteOppgaverDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
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
    private val azureGraphService: AzureGraphService,
    private val pepClient: PepClient,
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
        val reservasjon = Reservasjon(
            reservertTil = LocalDateTime.now().plusHours(24).forskyvReservasjonsDato(),
            reservertAv = ident, flyttetAv = null, flyttetTidspunkt = null, begrunnelse = null, oppgave = uuid
        )
        log.info("reserverer oppgave med $ident $uuid")

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
                true,
                reservasjon.reservertTil,
                reservertAvMeg(ident),
                null,
                null
            )
        } catch (e: java.lang.IllegalArgumentException) {
            log.warn(e.message)
            return OppgaveStatusDto(
                true,
                reservasjon.reservertTil,
                false,
                reservasjon.reservertAv,
                null
            )
        }
    }

    @KtorExperimentalAPI
    suspend fun søkFagsaker(query: String): List<FagsakDto> {
        if (query.length == 11) {
            var aktørId = pdlService.identifikator(query)
            if (!configuration.erIProd) {
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
                    if (!configuration.erIProd) {
                        aktorId = "1172507325105"
                    }
                    return oppgaveRepository.hentOppgaverMedAktorId(aktorId).filter {
                        if (!pepClient.harTilgangTilLesSak(
                                fagsakNummer = it.fagsakSaksnummer
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
        val oppgave = oppgaveRepository.hentOppgaveMedSaksnummer(query)
        if (oppgave != null) {
            if (!pepClient.harTilgangTilLesSak(
                    fagsakNummer = oppgave.fagsakSaksnummer
                )
            ) {
                settSkjermet(oppgave)
                return emptyList()
            }
            val person = pdlService.person(oppgave.aktorId)!!
            return listOf(
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
        return emptyList()
    }

    suspend fun reservertAvMeg(ident: String?): Boolean {
        return azureGraphService.hentIdentTilInnloggetBruker() == ident
    }

    @KtorExperimentalAPI
    suspend fun tilOppgaveDto(oppgave: Oppgave, reservasjon: Reservasjon?): OppgaveDto {

        val oppgaveStatus =
            if (reservasjon != null && (!reservasjon.erAktiv())) OppgaveStatusDto(false, null, false, null, null)
            else OppgaveStatusDto(
                true,
                reservasjon.reservertTil,
                reservertAvMeg(reservasjon.reservertAv),
                reservasjon.reservertAv,
                null
            )
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
        return saksnummere.map { oppgaveRepository.hentOppgaveMedSaksnummer(it) }
            .map { oppgave ->
                tilOppgaveDto(
                    oppgave!!, if (reservasjonRepository.finnes(oppgave.eksternId)
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
        return oppgaveKøRepository.hentOppgavekø(UUID.fromString(oppgavekoId)).nyeOgFerdigstilteOppgaverSisteSyvDager()
            .map { NyeOgFerdigstilteOppgaverDto(it.behandlingType, it.dato, it.nye.size, it.ferdigstilte.size) }
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
            )
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

    fun hentSisteBehandledeOppgaver(ident: String): List<BehandletOppgave> {
        return statistikkRepository.hentBehandlinger(ident)
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
                oppgaveKøRepository = oppgaveKøRepository,
                oppgaveRepository = oppgaveRepository
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
        if (configuration.erIkkeLokalt) {
            if (pepClient.harBasisTilgang()) {
                val list = mutableListOf<OppgaveDto>()
                val ms = measureTimeMillis {
                    for (oppgave in hentOppgaver(kø)) {
                        if (list.size == 10) {
                            break
                        }
                        if (!pepClient.harTilgangTilLesSak(
                                fagsakNummer = oppgave.fagsakSaksnummer
                            )
                        ) {
                            settSkjermet(oppgave)
                            continue
                        }
                        val person = pdlService.person(oppgave.aktorId)

                        val navn = if (configuration.erIDevFss) {
                            "${oppgave.fagsakSaksnummer} " + Strings.join(
                                oppgave.aksjonspunkter.liste.entries.stream().map { t ->
                                    val a = Aksjonspunkter().aksjonspunkter()
                                        .find { aksjonspunkt -> aksjonspunkt.kode == t.key }
                                    "${t.key} ${a?.navn ?: "Ukjent aksjonspunkt"}"
                                }.toList(),
                                ", "
                            )
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
    suspend fun hentSisteReserverteOppgaver(epost: String): List<OppgaveDto> {
        val list = mutableListOf<OppgaveDto>()
        //Hent reservasjoner for en gitt bruker skriv om til å hente med ident direkte i tabellen
        val saksbehandlerMedEpost = saksbehandlerRepository.finnSaksbehandlerMedEpost(epost)
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
                    null
                )
            var personNavn: String
            var personFnummer: String
            val navn = if (configuration.erIDevFss) {
                "${oppgave.fagsakSaksnummer} " + Strings.join(
                    oppgave.aksjonspunkter.liste.entries.stream().map { t ->
                        val a = Aksjonspunkter().aksjonspunkter()
                            .find { aksjonspunkt -> aksjonspunkt.kode == t.key }
                        "${t.key} ${a?.navn ?: "Ukjent aksjonspunkt"}"
                    }.toList(),
                    ", "
                )
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

    private suspend fun tilgangTilSak(oppgave: Oppgave): Boolean {
        if (!pepClient.harTilgangTilLesSak(
                fagsakNummer = oppgave.fagsakSaksnummer
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
