package no.nav.k9.tjenester.saksbehandler.oppgave

import io.ktor.util.KtorExperimentalAPI
import joptsimple.internal.Strings
import no.nav.k9.Configuration
import no.nav.k9.domene.lager.oppgave.BehandletOppgave
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.lager.oppgave.Reservasjon
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.OppgaveKø
import no.nav.k9.domene.modell.Saksbehandler
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.ReservasjonRepository
import no.nav.k9.domene.repository.SaksbehandlerRepository
import no.nav.k9.integrasjon.abac.PepClient
import no.nav.k9.integrasjon.pdl.AktøridPdl
import no.nav.k9.integrasjon.pdl.PdlService
import no.nav.k9.integrasjon.pdl.navn
import no.nav.k9.integrasjon.rest.idToken
import no.nav.k9.tjenester.avdelingsleder.oppgaveko.OppgavekøIdDto
import no.nav.k9.tjenester.fagsak.FagsakDto
import no.nav.k9.tjenester.fagsak.PersonDto
import no.nav.k9.tjenester.mock.Aksjonspunkter
import no.nav.k9.tjenester.saksbehandler.IdToken
import no.nav.k9.tjenester.saksbehandler.nokkeltall.NyeOgFerdigstilteOppgaverDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.coroutines.coroutineContext
import kotlin.streams.toList

private val log: Logger =
    LoggerFactory.getLogger(OppgaveTjeneste::class.java)

class OppgaveTjeneste @KtorExperimentalAPI constructor(
    private val oppgaveRepository: OppgaveRepository,
    private val oppgaveKøRepository: OppgaveKøRepository,
    private val saksbehandlerRepository: SaksbehandlerRepository,
    private val pdlService: PdlService,
    private val reservasjonRepository: ReservasjonRepository,
    private val configuration: Configuration,
    private val pepClient: PepClient
) {

    fun hentOppgaver(oppgavekøId: UUID): List<Oppgave> {
        return try {
            val oppgaveKø = oppgaveKøRepository.hentOppgavekø(oppgavekøId)
            oppgaveRepository.hentOppgaver(oppgaveKø.oppgaver.take(50))
        } catch (e: Exception) {
            log.error("Henting av oppgave feilet, returnerer en tom oppgaveliste", e)
            emptyList()
        }
    }

    fun reserverOppgave(ident: String, uuid: UUID): Reservasjon {
        val reservasjon = Reservasjon(
            reservertTil = LocalDateTime.now().plusHours(24).forskyvReservasjonsDato(),
            reservertAv = ident, flyttetAv = null, flyttetTidspunkt = null, begrunnelse = null, oppgave = uuid
        )
        reservasjonRepository.lagre(uuid) {
            reservasjon
        }
        val oppgave = oppgaveRepository.hent(uuid)
        for (oppgaveKø in oppgaveKøRepository.hent()) {
            oppgaveKøRepository.lagre(oppgaveKø.id) {
                it!!.leggOppgaveTilEllerFjernFraKø(oppgave, reservasjonRepository)
                it
            }
        }

        oppgaveRepository.lagreBehandling(ident){BehandletOppgave(
            oppgave.behandlingId,
            oppgave.fagsakSaksnummer,
            oppgave.eksternId,
            oppgave.aktorId
        )}

        return reservasjon
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
            if (aktørId != null) {
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
        return IdToken(coroutineContext.idToken().value).ident.value == ident
    }

    @KtorExperimentalAPI
    suspend fun tilOppgaveDto(oppgave: Oppgave, reservasjon: Reservasjon?): OppgaveDto {

        val oppgaveStatus = if (reservasjon == null) OppgaveStatusDto(false, null, false, null, null)
        else OppgaveStatusDto(
            true,
            reservasjon.reservertTil,
            reservertAvMeg(reservasjon.reservertAv),
            reservasjon.reservertAv,
            null
        )
        val person = pdlService.person(oppgave.aktorId)!!
        return OppgaveDto(
            oppgaveStatus,
            oppgave.behandlingId,
            oppgave.fagsakSaksnummer,
            person.navn(),
            oppgave.system,
            person.data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer,
            oppgave.behandlingType,
            oppgave.fagsakYtelseType,
            oppgave.behandlingStatus,
            oppgave.aktiv,
            oppgave.behandlingOpprettet,
            oppgave.behandlingsfrist,
            oppgave.eksternId,
            oppgave.tilBeslutter,
            oppgave.utbetalingTilBruker,
            oppgave.selvstendigFrilans,
            oppgave.kombinert,
            oppgave.søktGradering,
            oppgave.registrerPapir
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

    fun hentNyeOgFerdigstilteOppgaver(oppgavekoId: OppgavekøIdDto): List<NyeOgFerdigstilteOppgaverDto> {
        val kø = oppgaveKøRepository.hentOppgavekø(UUID.fromString(oppgavekoId.id))
        val køOppgaver = oppgaveRepository.hentOppgaverSortertPåOpprettetDato(kø.oppgaver)
        val liste = mutableListOf<NyeOgFerdigstilteOppgaverDto>()
        kø.filtreringBehandlingTyper.forEach {
            liste.add(
                NyeOgFerdigstilteOppgaverDto(
                    behandlingType = it,
                    antallNye = tellNyeOppgaver(it, køOppgaver),
                    antallFerdigstilte = tellFerdigstilteOppgaver(it, køOppgaver),
                    dato = LocalDate.now()
                )
            )
        }
        return liste
    }

    fun tellNyeOppgaver(behandlingType: BehandlingType, oppgaver: List<Oppgave>): Long {
        return oppgaver.count {
            it.behandlingType == behandlingType && it.behandlingOpprettet.toLocalDate() == LocalDate.now()
        }.toLong()
    }

    fun tellFerdigstilteOppgaver(behandlingType: BehandlingType, oppgaver: List<Oppgave>): Long {
        return oppgaver.filter { it.oppgaveAvsluttet != null }.count {
            it.behandlingType == behandlingType && it.oppgaveAvsluttet!!.toLocalDate() == LocalDate.now()
        }.toLong()
    }

    fun frigiReservasjon(uuid: UUID, begrunnelse: String): Reservasjon {
        val reservasjon = reservasjonRepository.lagre(uuid) {
            it!!.begrunnelse = begrunnelse
            it.reservertTil = null
            it
        }
        val oppgave = oppgaveRepository.hent(uuid)
        for (oppgaveKø in oppgaveKøRepository.hent()) {
            oppgaveKøRepository.lagre(oppgaveKø.id) {
                it!!.leggOppgaveTilEllerFjernFraKø(oppgave, reservasjonRepository)
                it
            }
        }
        return reservasjon
    }

    fun forlengReservasjonPåOppgave(uuid: UUID): Reservasjon {
        return reservasjonRepository.lagre(uuid) {
            it!!.reservertTil = it.reservertTil?.plusHours(24)!!.forskyvReservasjonsDato()
            it
        }
    }

    fun flyttReservasjon(uuid: UUID, ident: String, begrunnelse: String): Reservasjon {
        return reservasjonRepository.lagre(uuid) {
            it!!.reservertTil = it.reservertTil?.plusHours(24)!!.forskyvReservasjonsDato()
            it.flyttetTidspunkt = LocalDateTime.now()
            it.reservertAv = ident
            it.begrunnelse = begrunnelse
            it
        }
    }

    @KtorExperimentalAPI
   suspend fun hentSisteBehandledeOppgaver(ident: String): List<BehandletOppgaveDto> {
        return oppgaveRepository.hentBehandlinger(ident).map {
            val person = pdlService.person(it.aktørId)
            val navn = person?.navn() ?: "Ukjent navn"
            val fnummer = if(person == null) "Ukjent nummer" else person.data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer
            BehandletOppgaveDto(
                it.behandlingId,
                it.saksnummer,
                it.eksternId,
                fnummer,
                navn)
        }
    }

    fun flyttReservasjonTilForrigeSakbehandler(uuid: UUID) {
        val reservasjoner = reservasjonRepository.hentMedHistorikk(uuid).reversed()
        for (reservasjon in reservasjoner) {
            if (reservasjoner[0].reservertAv != reservasjon.reservertAv) {
                reservasjonRepository.lagre(uuid) {
                    it!!.reservertAv = reservasjon.reservertAv
                    it.reservertTil = LocalDateTime.now().plusDays(3).forskyvReservasjonsDato()
                    it
                }
                return
            }
        }
    }

    fun hentAntallOppgaver(oppgavekøId: UUID, taMedReserverte: Boolean = false): Int {
        val reservasjoner = reservasjonRepository.hent(
            oppgaveKøRepository = oppgaveKøRepository,
            oppgaveRepository = oppgaveRepository
        )
        val oppgavekø = oppgaveKøRepository.hentOppgavekø(oppgavekøId)
        var reserverteOppgaverSomHørerTilKø = 0
        if (taMedReserverte) {
            for (oppgave in oppgaveRepository.hentOppgaver(reservasjoner.map { it.oppgave })) {
                if (oppgavekø.tilhørerOppgaveTilKø(oppgave, reservasjonRepository, false)) {
                    reserverteOppgaverSomHørerTilKø++
                }
            }
        }
        return oppgavekø.oppgaver.size + reserverteOppgaverSomHørerTilKø
    }

    fun hentAntallOppgaverTotalt(): Int {
        return oppgaveRepository.hentAktiveOppgaverTotalt()
    }

    @KtorExperimentalAPI
    suspend fun hentNesteOppgaverIKø(kø: UUID): List<OppgaveDto> {
        if (configuration.erIkkeLokalt) {
            if (pepClient.harBasisTilgang()) {
                val list = mutableListOf<OppgaveDto>()
                val oppgaver = hentOppgaver(kø)
                for (oppgave in oppgaver) {
                    if (list.size == 3) {
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
                                reservertAvUid = null,
                                flyttetReservasjon = null
                            ),
                            behandlingId = oppgave.behandlingId,
                            saksnummer = oppgave.fagsakSaksnummer,
                            navn = navn,
                            system = oppgave.system,
                            personnummer = if (person == null) {
                                "Ukent fnummer"
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
                return list
            } else {
                log.info("har ikke basistilgang")
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
                            reservertAvUid = null,
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
        for (reservasjon in reservasjonRepository.hent(
            oppgaveKøRepository = oppgaveKøRepository,
            oppgaveRepository = oppgaveRepository
        )
            .filter {
                saksbehandlerMedEpost != null && it.reservertAv == saksbehandlerMedEpost.brukerIdent
            }) {
            val oppgave = oppgaveRepository.hent(reservasjon.oppgave)
            if (!pepClient.harTilgangTilLesSak(
                    fagsakNummer = oppgave.fagsakSaksnummer
                )
            ) {
                reservasjonRepository.lagre(oppgave.eksternId) {
                    if (saksbehandlerMedEpost!!.brukerIdent == it!!.reservertAv) {
                        it.reservertTil = null
                    }
                    it
                }
                settSkjermet(oppgave)
                oppgaveKøRepository.oppdaterKøMedOppgaver(oppgave.eksternId)
                continue
            }

            val person = pdlService.person(oppgave.aktorId)


            val status =
                OppgaveStatusDto(
                    true,
                    reservasjon.reservertTil,
                    true,
                    reservasjon.reservertAv,
                    null
                )
            var personNavn = "Ukjent navn"
            var personFnummer = "Ukjent fnummer"
            if (person != null) {
                personNavn = person.navn()
                personFnummer = person.data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer
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

    fun sokSaksbehandlerMedIdent(ident: BrukerIdentDto): Saksbehandler? {
        return saksbehandlerRepository.finnSaksbehandlerMedIdent(ident.brukerIdent)
    }

    fun hentOppgaveKøer(): List<OppgaveKø> {
        return oppgaveKøRepository.hent()
    }

    fun settSkjermet(oppgave: Oppgave) {
        oppgaveRepository.lagre(oppgave.eksternId, f = { forrigeOppgave ->
            forrigeOppgave?.skjermet = true
            forrigeOppgave!!
        })
    }
}
