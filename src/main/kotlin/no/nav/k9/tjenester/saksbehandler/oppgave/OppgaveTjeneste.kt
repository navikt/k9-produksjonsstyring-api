package no.nav.k9.tjenester.saksbehandler.oppgave

import info.debatty.java.stringsimilarity.Levenshtein
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import no.nav.k9.Configuration
import no.nav.k9.KoinProfile
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.lager.oppgave.Reservasjon
import no.nav.k9.domene.modell.BehandlingStatus
import no.nav.k9.domene.modell.FagsakYtelseType
import no.nav.k9.domene.modell.OppgaveKø
import no.nav.k9.domene.modell.Saksbehandler
import no.nav.k9.domene.repository.*
import no.nav.k9.integrasjon.abac.IPepClient
import no.nav.k9.integrasjon.azuregraph.IAzureGraphService
import no.nav.k9.integrasjon.kafka.dto.Fagsystem
import no.nav.k9.integrasjon.omsorgspenger.IOmsorgspengerService
import no.nav.k9.integrasjon.omsorgspenger.OmsorgspengerService
import no.nav.k9.integrasjon.pdl.*
import no.nav.k9.integrasjon.rest.idToken
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleOppgaverHistorikk
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleOppgaverNyeOgFerdigstilte
import no.nav.k9.tjenester.fagsak.FagsakDto
import no.nav.k9.tjenester.fagsak.PersonDto
import no.nav.k9.tjenester.mock.Aksjonspunkter
import no.nav.k9.tjenester.saksbehandler.nokkeltall.NyeOgFerdigstilteOppgaverDto
import no.nav.k9.utils.Cache
import no.nav.k9.utils.CacheObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import kotlin.coroutines.coroutineContext
import kotlin.streams.toList
import kotlin.system.measureTimeMillis

private val log: Logger =
    LoggerFactory.getLogger(OppgaveTjeneste::class.java)

@KtorExperimentalAPI
class OppgaveTjeneste @KtorExperimentalAPI constructor(
    private val oppgaveRepository: OppgaveRepository,
    private val oppgaveKøRepository: OppgaveKøRepository,
    private val saksbehandlerRepository: SaksbehandlerRepository,
    private val pdlService: IPdlService,
    private val reservasjonRepository: ReservasjonRepository,
    private val configuration: Configuration,
    private val azureGraphService: IAzureGraphService,
    private val pepClient: IPepClient,
    private val statistikkRepository: StatistikkRepository,
    private val omsorgspengerService: IOmsorgspengerService

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
                reservasjon
            }
            saksbehandlerRepository.leggTilReservasjon(reservasjon.reservertAv, reservasjon.oppgave)
            val oppgave = oppgaveRepository.hent(uuid)

            for (oppgavekø in oppgaveKøRepository.hentKøIdIkkeTaHensyn()) {
                oppgaveKøRepository.leggTilOppgaverTilKø(oppgavekø, listOf(oppgave), reservasjonRepository)
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
    suspend fun søkFagsaker(query: String): SokeResultatDto {
        if (query.length == 11) {
            var aktørId = pdlService.identifikator(query)
            if (configuration.koinProfile() != KoinProfile.PROD) {
                aktørId = PdlResponse(
                    false, AktøridPdl(
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
                )
            }
            if (aktørId.aktorId != null && aktørId.aktorId!!.data.hentIdenter != null && aktørId.aktorId!!.data.hentIdenter!!.identer.isNotEmpty()) {
                var aktorId = aktørId.aktorId!!.data.hentIdenter!!.identer[0].ident
                val person = pdlService.person(aktorId)
                if (person.person != null) {
                    if (!(configuration.koinProfile() == KoinProfile.PROD)) {
                        aktorId = "1172507325105"
                    }
                    val result = hentOppgaver(aktorId, person.person)
                    return SokeResultatDto(aktørId.ikkeTilgang, result)
                } else {
                    return SokeResultatDto(person.ikkeTilgang, mutableListOf())
                }
            }
        }
        val ret = mutableListOf<FagsakDto>()

        val omsorgspengerSakDto = omsorgspengerService.hentOmsorgspengerSakDto(query)
        log.info("Fikk dette som svar fra omsorgsdager", omsorgspengerSakDto)

        if (omsorgspengerSakDto != null) {
            ret.add(
                FagsakDto(
                    Fagsystem.OMSORGSPENGER,
                    omsorgspengerSakDto.saksnummer,
                    PersonDto(
                        "Ukjent navn",
                        "",
                        "",
                        null
                    ),
                    FagsakYtelseType.OMSORGSPENGER,
                    BehandlingStatus.OPPRETTET,
                    LocalDateTime.now(),
                    true
                )
            )
        }


        val oppgaver = oppgaveRepository.hentOppgaverMedSaksnummer(query)
        for (oppgave in oppgaver) {
            if (!pepClient.harTilgangTilLesSak(
                    fagsakNummer = oppgave.fagsakSaksnummer,
                    aktørid = oppgave.aktorId
                )
            ) {
                settSkjermet(oppgave)
                return SokeResultatDto(true, mutableListOf())
            }
            val person = pdlService.person(oppgave.aktorId)

            ret.add(
                FagsakDto(
                    Fagsystem.fraKode(oppgave.system),
                    oppgave.fagsakSaksnummer,
                    if (person.person != null) {
                        PersonDto(
                            person.person.navn(),
                            person.person.data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer,
                            person.person.data.hentPerson.kjoenn[0].kjoenn,
                            null
                            // person.data.hentPerson.doedsfall!!.doedsdato
                        )
                    } else {
                        PersonDto(
                            "Ukjent navn",
                            "",
                            "",
                            null
                        )
                        // person.data.hentPerson.doedsfall!!.doedsdato
                    },

                    oppgave.fagsakYtelseType,
                    oppgave.behandlingStatus,
                    oppgave.behandlingOpprettet,
                    oppgave.aktiv
                )
            )
        }
        return SokeResultatDto(false, ret)
    }

    private suspend fun hentOppgaver(aktorId: String, person: PersonPdl): MutableList<FagsakDto> {
        return oppgaveRepository.hentOppgaverMedAktorId(aktorId).filter { oppgave ->
            if (!pepClient.harTilgangTilLesSak(
                    fagsakNummer = oppgave.fagsakSaksnummer,
                    aktørid = oppgave.aktorId
                )
            ) {
                settSkjermet(oppgave)
                false
            } else {
                true
            }
        }.map {
            FagsakDto(
                Fagsystem.K9SAK,
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
        }.toMutableList()
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
        val person = pdlService.person(oppgave.aktorId)
        return OppgaveDto(
            status = oppgaveStatus,
            behandlingId = oppgave.behandlingId,
            journalpostId = oppgave.journalpostId,
            saksnummer = oppgave.fagsakSaksnummer,
            navn = person.person?.navn() ?: "Ukjent navn",
            system = oppgave.system,
            personnummer = if (person.person != null) {
                person.person.data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer
            } else {
                "Ukjent fnummer"
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

    @KtorExperimentalAPI
    suspend fun hentNyeOgFerdigstilteOppgaver(): List<NyeOgFerdigstilteOppgaverDto> {
        val slåttSammenAlleFagsakYtelser = mutableListOf<AlleOppgaverNyeOgFerdigstilte>()
        for (entry in statistikkRepository.hentFerdigstilteOgNyeHistorikkPerAntallDager(7).groupBy { it.dato }) {
            entry.value.groupBy { it.behandlingType }.forEach { behandlingType ->
                slåttSammenAlleFagsakYtelser.add(behandlingType.value.reduce { acc, alleOppgaverNyeOgFerdigstilte ->
                    acc.nye.addAll(alleOppgaverNyeOgFerdigstilte.nye)
                    acc.ferdigstilte.addAll(alleOppgaverNyeOgFerdigstilte.ferdigstilte)
                    acc.ferdigstilteSaksbehandler.addAll(alleOppgaverNyeOgFerdigstilte.ferdigstilteSaksbehandler)
                    acc
                })
            }
        }

        return slåttSammenAlleFagsakYtelser.map {
            val hentIdentTilInnloggetBruker = azureGraphService.hentIdentTilInnloggetBruker()
            val antallFerdistilteMine =
                reservasjonRepository.hentSelvOmDeIkkeErAktive(it.ferdigstilte.map { UUID.fromString(it)!! }
                    .toSet())
                    .filter { it.reservertAv == hentIdentTilInnloggetBruker }.size
            NyeOgFerdigstilteOppgaverDto(
                behandlingType = it.behandlingType,
                dato = it.dato,
                antallNye = it.nye.size,
                antallFerdigstilte = it.ferdigstilteSaksbehandler.size,
                antallFerdigstilteMine = antallFerdistilteMine
            )
        }
    }

    fun hentBeholdningAvOppgaverPerAntallDager(): List<AlleOppgaverHistorikk> {
        val ytelsetype =
            statistikkRepository.hentFerdigstilteOgNyeHistorikkMedYtelsetypeSiste8Uker()
        val ret = mutableListOf<AlleOppgaverHistorikk>()
        for (ytelseTypeEntry in ytelsetype.groupBy { it.fagsakYtelseType }) {
            val perBehandlingstype = ytelseTypeEntry.value.groupBy { it.behandlingType }
            for (behandlingTypeEntry in perBehandlingstype) {
                var aktive =
                    oppgaveRepository.hentAktiveOppgaverTotaltPerBehandlingstypeOgYtelseType(
                        fagsakYtelseType = ytelseTypeEntry.key,
                        behandlingType = behandlingTypeEntry.key
                    )
                behandlingTypeEntry.value.sortedByDescending { it.dato }.map {
                    aktive = aktive - it.nye.size + it.ferdigstilte.size
                    ret.add(
                        AlleOppgaverHistorikk(
                            it.fagsakYtelseType,
                            it.behandlingType,
                            it.dato,
                            aktive
                        )
                    )
                }
            }
        }
        return ret
    }

    suspend fun frigiReservasjon(uuid: UUID, begrunnelse: String): Reservasjon {
        val reservasjon = reservasjonRepository.lagre(uuid, true) {
            it!!.begrunnelse = begrunnelse
            it.reservertTil = null
            it
        }
        saksbehandlerRepository.fjernReservasjon(reservasjon.reservertAv, reservasjon.oppgave)
        val oppgave = oppgaveRepository.hent(uuid)
        for (oppgavekø in oppgaveKøRepository.hent()) {
            oppgaveKøRepository.leggTilOppgaverTilKø(oppgavekø.id, listOf(oppgave), reservasjonRepository)
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
        val reservasjon = reservasjonRepository.hent(uuid)
        saksbehandlerRepository.fjernReservasjon(reservasjon.reservertAv, reservasjon.oppgave)
        saksbehandlerRepository.leggTilReservasjon(ident, reservasjon.oppgave)
        return reservasjonRepository.lagre(uuid, true) {
            if (it!!.reservertTil == null) {
                it.reservertTil = LocalDateTime.now().plusHours(24).forskyvReservasjonsDato()
            } else {
                it.reservertTil = it.reservertTil?.plusHours(24)!!.forskyvReservasjonsDato()
            }
            it.flyttetTidspunkt = LocalDateTime.now()
            it.reservertAv = ident
            it.flyttetAv = hentIdentTilInnloggetBruker
            it.begrunnelse = begrunnelse
            it
        }
    }

    suspend fun hentSisteBehandledeOppgaver(): List<BehandletOppgave> {
        return statistikkRepository.hentBehandlinger(coroutineContext.idToken().getUsername())
    }

    suspend fun flyttReservasjonTilForrigeSakbehandler(uuid: UUID) {
        val reservasjoner = reservasjonRepository.hentMedHistorikk(uuid).reversed()
        for (reservasjon in reservasjoner) {
            if (reservasjoner[0].reservertAv != reservasjon.reservertAv) {
                reservasjonRepository.lagre(uuid, true) {

                    it!!.reservertAv = reservasjon.reservertAv
                    it.reservertTil = LocalDateTime.now().plusDays(3).forskyvReservasjonsDato()
                    it
                }

                saksbehandlerRepository.fjernReservasjon(reservasjon.reservertAv, reservasjon.oppgave)
                saksbehandlerRepository.leggTilReservasjon(
                    reservasjon.reservertAv,
                    reservasjon.oppgave
                )
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

    private val hentAntallOppgaverCache = Cache<Int>()
    fun hentAntallOppgaver(oppgavekøId: UUID, taMedReserverte: Boolean = false, refresh: Boolean = false): Int {
        val key = oppgavekøId.toString() + taMedReserverte
        if (!refresh) {
            val cacheObject = hentAntallOppgaverCache.get(key)
            if (cacheObject != null) {
                return cacheObject.value
            }
        }
        val oppgavekø = oppgaveKøRepository.hentOppgavekø(oppgavekøId)
        var reserverteOppgaverSomHørerTilKø = 0
        if (taMedReserverte) {
            val reservasjoner = reservasjonRepository.hentSelvOmDeIkkeErAktive(
                saksbehandlerRepository.hentAlleSaksbehandlereIkkeTaHensyn()
                    .flatMap { saksbehandler -> saksbehandler.reservasjoner }.toSet()
            )

            for (oppgave in oppgaveRepository.hentOppgaver(reservasjoner.map { it.oppgave })) {
                if (oppgavekø.tilhørerOppgaveTilKø(oppgave, reservasjonRepository)) {
                    reserverteOppgaverSomHørerTilKø++
                }
            }
        }
        val antall = oppgavekø.oppgaverOgDatoer.size + reserverteOppgaverSomHørerTilKø
        hentAntallOppgaverCache.set(key, CacheObject(antall, LocalDateTime.now().plusMinutes(30)))
        return antall
    }

    suspend fun hentAntallOppgaverTotalt(): Int {
        return oppgaveRepository.hentAktiveOppgaverTotalt()
    }

    @KtorExperimentalAPI
    suspend fun hentNesteOppgaverIKø(kø: UUID): List<OppgaveDto> {
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
                        preprodNavn(oppgave)
                    } else {
                        person.person?.navn() ?: "Uten navn"
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
                            journalpostId = oppgave.journalpostId,
                            navn = navn,
                            system = oppgave.system,
                            personnummer = if (person.person == null) {
                                "Ukjent fnummer"
                            } else {
                                person.person.data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer
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
        }
        return emptyList()
    }

    private fun preprodNavn(oppgave: Oppgave): String {
        return "${oppgave.fagsakSaksnummer} " +
                oppgave.aksjonspunkter.liste.entries.stream().map { t ->
                    val a = Aksjonspunkter().aksjonspunkter()
                        .find { aksjonspunkt -> aksjonspunkt.kode == t.key }
                    "${t.key} ${a?.navn ?: "Ukjent aksjonspunkt"}"
                }.toList().joinToString(", ")
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
                person.person?.navn() ?: "Uten navn"
            }
            personNavn = navn
            personFnummer = if (person.person == null) {
                "Ukjent fnummer"
            } else {
                person.person.data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer
            }
            list.add(
                OppgaveDto(
                    status = status,
                    behandlingId = oppgave.behandlingId,
                    saksnummer = oppgave.fagsakSaksnummer,
                    journalpostId = oppgave.journalpostId,
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
                runBlocking { saksbehandlerRepository.fjernReservasjon(it.reservertAv, it.oppgave) }
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

    suspend fun sokSaksbehandlerMedIdent(ident: BrukerIdentDto): Saksbehandler? {
        return saksbehandlerRepository.finnSaksbehandlerMedIdent(ident.brukerIdent)
    }

    suspend fun sokSaksbehandler(søkestreng: String): Saksbehandler? {
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

    suspend fun hentOppgaveKøer(): List<OppgaveKø> {
        return oppgaveKøRepository.hent()
    }

    @KtorExperimentalAPI
    fun leggTilBehandletOppgave(ident: String, oppgave: BehandletOppgave) {
        return statistikkRepository.lagreBehandling(ident) {
            oppgave
        }
    }

    suspend fun settSkjermet(oppgave: Oppgave) {
        oppgaveRepository.lagre(oppgave.eksternId) {
            it!!
        }
        val oppaveSkjermet = oppgaveRepository.hent(oppgave.eksternId)
        for (oppgaveKø in oppgaveKøRepository.hent()) {
            val skalOppdareKø = oppgaveKø.leggOppgaveTilEllerFjernFraKø(oppaveSkjermet, reservasjonRepository)
            if (skalOppdareKø) {
                oppgaveKøRepository.lagre(oppgaveKø.id) {
                    it!!.leggOppgaveTilEllerFjernFraKø(oppaveSkjermet, reservasjonRepository)
                    it
                }
            }
        }
    }
}
