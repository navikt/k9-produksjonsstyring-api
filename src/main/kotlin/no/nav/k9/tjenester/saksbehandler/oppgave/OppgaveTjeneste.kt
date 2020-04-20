package no.nav.k9.tjenester.saksbehandler.oppgave

//import no.nav.k9.integrasjon.K9SakRestKlient
import no.nav.k9.domene.lager.aktør.TpsPersonDto
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.lager.oppgave.Reservasjon
import no.nav.k9.domene.modell.OppgaveKø
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.integrasjon.pdl.PdlService
import no.nav.k9.integrasjon.rest.idToken
import no.nav.k9.tilgangskontroll.log
import no.nav.k9.tjenester.saksbehandler.IdToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import kotlin.coroutines.coroutineContext
import kotlin.streams.toList


private val LOGGER: Logger =
    LoggerFactory.getLogger(OppgaveTjeneste::class.java)


class OppgaveTjeneste(
    private val oppgaveRepository: OppgaveRepository,
    private val oppgaveKøRepository: OppgaveKøRepository,
    private val pdlService: PdlService
) {

    fun hentOppgaver(oppgavekøId: UUID): List<Oppgave> {
        return try {
            val oppgaveKø = oppgaveKøRepository.hentOppgavekø(oppgavekøId)
            val alleOppgaver = oppgaveRepository.hent().stream().filter { t -> t.sisteOppgave().reservasjon?.reservertAv.isNullOrEmpty() }
                .map { t -> t.sisteOppgave() }.toList()
            alleOppgaver.filter {
                it.behandlingType in oppgaveKø.filtreringBehandlingTyper &&
                        it.fagsakYtelseType in oppgaveKø.filtreringYtelseTyper &&
                        it.behandlingOpprettet.toLocalDate() >= oppgaveKø.fomDato &&
                        it.behandlingOpprettet.toLocalDate() <= oppgaveKø.tomDato &&
                        it.tilBeslutter == oppgaveKø.tilBeslutter &&
                        it.utbetalingTilBruker == oppgaveKø.utbetalingTilBruker &&
                        it.selvstendigFrilans == oppgaveKø.selvstendigFrilans &&
                        it.kombinert == oppgaveKø.kombinert &&
                        it.søktGradering == oppgaveKø.søktGradering &&
                        it.registrerPapir == oppgaveKø.registrerPapir
            }
        } catch (e: Exception) {
            LOGGER.error("Henting av oppgave feilet, returnerer en tom oppgaveliste", e)
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
            ident, null, null, null
        )

        oppgaveRepository.lagre(uuid) { forrigeOppgave ->
            forrigeOppgave?.reservasjon = reservasjon
            forrigeOppgave!!
        }

        return reservasjon
    }

    fun hentReservasjon(uuid: UUID): Reservasjon {
        return oppgaveRepository.hent(uuid).sisteOppgave().reservasjon!!
    }

    fun frigiOppgave(uuid: UUID, begrunnelse: String): Reservasjon {
        var reservasjon: Reservasjon? = null
        oppgaveRepository.lagre(uuid) { forrigeOppgave ->
            forrigeOppgave?.reservasjon?.reservertAv = ""
            forrigeOppgave?.reservasjon?.begrunnelse = begrunnelse
            reservasjon = forrigeOppgave?.reservasjon!!
            forrigeOppgave
        }

        return reservasjon!!
    }

    fun forlengReservasjonPåOppgave(uuid: UUID): Reservasjon {
        var reservasjon: Reservasjon? = null
        oppgaveRepository.lagre(uuid) { forrigeOppgave ->
            forrigeOppgave?.reservasjon?.reservertTil = forrigeOppgave?.reservasjon?.reservertTil?.plusHours(24)
            reservasjon = forrigeOppgave?.reservasjon
            forrigeOppgave!!
        }

        return reservasjon!!
    }

    fun flyttReservasjon(uuid: UUID, ident: String, begrunnelse: String): Reservasjon {
        var reservasjon: Reservasjon? = null
        oppgaveRepository.lagre(uuid) { forrigeOppgave ->
            forrigeOppgave?.reservasjon?.reservertTil = forrigeOppgave?.reservasjon?.reservertTil?.plusHours(24)
            forrigeOppgave?.reservasjon?.flyttetTidspunkt = LocalDateTime.now()
            forrigeOppgave?.reservasjon?.reservertAv = ident
            forrigeOppgave?.reservasjon?.begrunnelse = begrunnelse
            reservasjon = forrigeOppgave?.reservasjon
            forrigeOppgave!!
        }

        return reservasjon!!
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
        return hentOppgaver(oppgavekøId).size
    }

    fun hentAntallOppgaverTotalt(): Int {
        return oppgaveRepository.hent().size
    }

    suspend fun hentSisteReserverteOppgaver(ident: String): List<OppgaveDto> {
        val reserverteOppgave = oppgaveRepository.hentReserverteOppgaver(ident)
        val list = mutableListOf<OppgaveDto>()
        val token = IdToken(coroutineContext.idToken().value)
        for (oppgavemodell in reserverteOppgave) {
            val person = pdlService.person(oppgavemodell.sisteOppgave().aktorId)
            if (person == null) {
                // Flytt oppgave til vikafossen
                log.info("Ikke tilgang til bruker: " + ident)
                continue
            }
            list.add(
                OppgaveDto(
                    OppgaveStatusDto(
                        true, oppgavemodell.sisteOppgave().reservasjon?.reservertTil,
                        true, oppgavemodell.sisteOppgave().reservasjon?.reservertAv, token.getName(), null
                    ),
                    oppgavemodell.sisteOppgave().behandlingId,
                    oppgavemodell.sisteOppgave().fagsakSaksnummer,
                    person.data.hentPerson.navn[0].forkortetNavn,
                    oppgavemodell.sisteOppgave().system,
                    person.data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer,
                    oppgavemodell.sisteOppgave().behandlingType,
                    oppgavemodell.sisteOppgave().fagsakYtelseType,
                    oppgavemodell.sisteOppgave().behandlingStatus,
                    true,
                    oppgavemodell.sisteOppgave().behandlingOpprettet,
                    oppgavemodell.sisteOppgave().behandlingsfrist,
                    oppgavemodell.sisteOppgave().eksternId,
                    oppgavemodell.sisteOppgave().tilBeslutter,
                    oppgavemodell.sisteOppgave().utbetalingTilBruker,
                    oppgavemodell.sisteOppgave().selvstendigFrilans,
                    oppgavemodell.sisteOppgave().kombinert,
                    oppgavemodell.sisteOppgave().søktGradering,
                    oppgavemodell.sisteOppgave().registrerPapir
                )
            )
        }
        return list
    }

    fun hentSaksbehandlerNavnOgAvdelinger(ident: String): SaksbehandlerinformasjonDto {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun hentNavnHvisReservertAvAnnenSaksbehandler(reservasjon: Reservasjon): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun hentNavnHvisFlyttetAvSaksbehandler(flyttetAv: String): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
