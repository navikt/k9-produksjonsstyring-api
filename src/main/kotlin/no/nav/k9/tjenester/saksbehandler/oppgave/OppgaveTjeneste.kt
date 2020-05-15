package no.nav.k9.tjenester.saksbehandler.oppgave

import io.ktor.util.KtorExperimentalAPI
import joptsimple.internal.Strings
import no.nav.k9.Configuration
import no.nav.k9.domene.lager.aktør.TpsPersonDto
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.lager.oppgave.Reservasjon
import no.nav.k9.domene.modell.KøSortering
import no.nav.k9.domene.modell.OppgaveKø
import no.nav.k9.domene.modell.Saksbehandler
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.repository.ReservasjonRepository
import no.nav.k9.domene.repository.SaksbehandlerRepository
import no.nav.k9.integrasjon.abac.PepClient
import no.nav.k9.integrasjon.pdl.PdlService
import no.nav.k9.integrasjon.rest.idToken
import no.nav.k9.tjenester.fagsak.FagsakDto
import no.nav.k9.tjenester.fagsak.PersonDto
import no.nav.k9.tjenester.mock.Aksjonspunkter
import no.nav.k9.tjenester.saksbehandler.IdToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
            when (oppgaveKø.sortering) {
                KøSortering.OPPRETT_BEHANDLING -> oppgaveRepository.hentOppgaverSortertPåOpprettetDato(oppgaveKø.oppgaver)
                KøSortering.FORSTE_STONADSDAG -> oppgaveRepository.hentOppgaverSortertPåFørsteStønadsdag(oppgaveKø.oppgaver)
            }
        } catch (e: Exception) {
            log.error("Henting av oppgave feilet, returnerer en tom oppgaveliste", e)
            emptyList()
        }
    }

    fun hentOppgaverForSaksnummer(fagsakSaksnummer: Long): List<Oppgave> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun hentAktiveOppgaverForSaksnummer(fagsakSaksnummerListe: Collection<Long>): List<Oppgave> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun hentReservasjonerTilknyttetAktiveOppgaver(): List<Reservasjon> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun reserverOppgave(ident: String, uuid: UUID): Reservasjon {

        val reservasjon = Reservasjon(
            LocalDateTime.now().plusHours(24),
            ident, null, null, null, oppgave = uuid
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

        return reservasjon
    }

    fun hentReservasjon(uuid: UUID): Reservasjon {
        return reservasjonRepository.hent(uuid)
    }

    @KtorExperimentalAPI
    suspend fun søkFagsaker(query: String): List<FagsakDto> {
        val aktørId = pdlService.identifikator(query)
        if (aktørId != null) {
            val aktorId = aktørId.data.hentIdenter.identer[0].ident
            val person = pdlService.person(aktorId)
            if (person != null) {
                return oppgaveRepository.hentOppgaverMedAktorId(aktorId).map {
                    FagsakDto(
                        it.fagsakSaksnummer,
                        PersonDto(
                            person.data.hentPerson.navn[0].forkortetNavn,
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
        val oppgave = oppgaveRepository.hentOppgaveMedSaksnummer(query)
        if (oppgave != null) {
            val person = pdlService.person(oppgave.aktorId)!!
            return listOf(
                FagsakDto(
                    oppgave.fagsakSaksnummer,
                    PersonDto(
                        person.data.hentPerson.navn[0].forkortetNavn,
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
            person.data.hentPerson.navn[0].forkortetNavn,
            oppgave.system,
            person.data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer,
            oppgave.behandlingType,
            oppgave.fagsakYtelseType,
            oppgave.behandlingStatus,
            true,
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


    suspend fun hentOppgaverFraListe(saksnummere: List<String>): List<OppgaveDto> {
        return saksnummere.map { oppgaveRepository.hentOppgaveMedSaksnummer(it) }
            .map { oppgave -> tilOppgaveDto(oppgave!!, null) }.toList()
    }

    fun frigiReservasjon(uuid: UUID, begrunnelse: String): Reservasjon {
        val reservasjon = reservasjonRepository.lagre(uuid) {
            it!!.begrunnelse = begrunnelse
            it.aktiv = false
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
            it!!.reservertTil = it.reservertTil?.plusHours(24)
            it
        }
    }

    fun flyttReservasjon(uuid: UUID, ident: String, begrunnelse: String): Reservasjon {
        return reservasjonRepository.lagre(uuid) {
            it!!.reservertTil = it.reservertTil?.plusHours(24)
            it.flyttetTidspunkt = LocalDateTime.now()
            it.reservertAv = ident
            it.begrunnelse = begrunnelse
            it
        }
    }

    fun hentAlleOppgaveKøer(ident: String): List<OppgaveKø> {
        return oppgaveKøRepository.hent().filter { oppgaveKø ->
            oppgaveKø.saksbehandlere.any { saksbehandler -> saksbehandler.brukerIdent == ident }
        }
    }

    fun hentPersonInfoOptional(aktørId: Long): Optional<TpsPersonDto> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun hentAntallOppgaver(oppgavekøId: UUID): Int {
        return oppgaveKøRepository.hentOppgavekø(oppgavekøId).oppgaver.size
    }

    fun hentAntallOppgaverTotalt(): Int {
        return oppgaveRepository.hentAktiveOppgaverTotalt()
    }

    @KtorExperimentalAPI
    suspend fun hentNesteOppgaverIKø(idToken: IdToken? = null, kø: UUID): List<OppgaveDto> {
        if (configuration.erIkkeLokalt) {
            if (pepClient.harBasisTilgang(idToken!!)) {
                val list = mutableListOf<OppgaveDto>()
                val oppgaver = hentOppgaver(kø)
                for (oppgave in oppgaver) {
                    if (list.size == 3) {
                        break
                    }
                    if (!pepClient.harTilgangTilLesSak(
                            idToken = idToken!!,
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
                        person!!.data.hentPerson.navn[0].forkortetNavn
                    }

                    list.add(
                        OppgaveDto(
                            OppgaveStatusDto(false, null, false, null, null),
                            oppgave.behandlingId,
                            oppgave.fagsakSaksnummer,
                            navn,
                            oppgave.system,
                            person!!.data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer,
                            oppgave.behandlingType,
                            oppgave.fagsakYtelseType,
                            oppgave.behandlingStatus,
                            true,
                            oppgave.behandlingOpprettet,
                            oppgave.behandlingsfrist,
                            oppgave.eksternId,
                            tilBeslutter = false,
                            utbetalingTilBruker = false,
                            søktGradering = false,
                            selvstendigFrilans = false,
                            registrerPapir = false,
                            kombinert = false
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
                        OppgaveStatusDto(false, null, false, null, null),
                        oppgave.behandlingId,
                        oppgave.fagsakSaksnummer,
                        "Navn",
                        oppgave.system,
                        oppgave.aktorId,
                        oppgave.behandlingType,
                        oppgave.fagsakYtelseType,
                        oppgave.behandlingStatus,
                        true,
                        oppgave.behandlingOpprettet,
                        oppgave.behandlingsfrist,
                        oppgave.eksternId,
                        tilBeslutter = false,
                        utbetalingTilBruker = false,
                        søktGradering = false,
                        selvstendigFrilans = false,
                        registrerPapir = false,
                        kombinert = false
                    )
                )
            }
            return list
        }
    }

    suspend fun hentSisteReserverteOppgaver(username: String): List<OppgaveDto> {

        val list = mutableListOf<OppgaveDto>()

        for (reservasjon in reservasjonRepository.hent().filter { it.erAktiv(reservasjonRepository) }
            .filter { it.reservertAv?.toLowerCase() == username.toLowerCase() }) {
            val oppgave = oppgaveRepository.hent(reservasjon.oppgave)
            val person = pdlService.person(oppgave.aktorId)
            if (person == null) {
                settSkjermet(oppgave = oppgave)
                log.info("Ikke tilgang til bruker: ${oppgave.aktorId}")
                continue
            }
            val status = if (username == "saksbehandler@nav.no") {
                OppgaveStatusDto(
                    true,
                    reservasjon.reservertTil,
                    true,
                    reservasjon.reservertAv,
                    null
                )
            } else {
                OppgaveStatusDto(
                    true,
                    reservasjon.reservertTil,
                    true,
                    reservasjon.reservertAv,
                    null
                )
            }

            list.add(
                OppgaveDto(
                    status,
                    oppgave.behandlingId,
                    oppgave.fagsakSaksnummer,
                    person.data.hentPerson.navn[0].forkortetNavn,
                    oppgave.system,
                    person.data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer,
                    oppgave.behandlingType,
                    oppgave.fagsakYtelseType,
                    oppgave.behandlingStatus,
                    true,
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
            )
        }
        return list
    }

    fun sokSaksbehandlerMedIdent(ident: BrukerIdentDto): Saksbehandler? {
        return saksbehandlerRepository.finnSaksbehandlerMedIdent(ident.brukerIdent)
    }

    fun hentNavnHvisReservertAvAnnenSaksbehandler(reservasjon: Reservasjon): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun hentNavnHvisFlyttetAvSaksbehandler(flyttetAv: String): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
