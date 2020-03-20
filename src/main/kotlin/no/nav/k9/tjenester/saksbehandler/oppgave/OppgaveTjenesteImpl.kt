package no.nav.k9.tjenester.saksbehandler.oppgave

//import no.nav.k9.integrasjon.K9SakRestKlient
import no.nav.k9.domene.lager.aktør.TpsPersonDto
import no.nav.k9.domene.lager.oppgave.*
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.repository.OppgaveRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import kotlin.streams.toList


private val LOGGER: Logger =
    LoggerFactory.getLogger(OppgaveTjenesteImpl::class.java)


class OppgaveTjenesteImpl(
    private val oppgaveRepository: OppgaveRepository
) : OppgaveTjeneste {

    override fun hentOppgaver(sakslisteId: Long): List<Oppgave> {
        return try {
            oppgaveRepository.hent().stream().map { t -> t.sisteOppgave() }.toList()
        } catch (e: Exception) {
            LOGGER.error("Henting av oppgave feilet, returnerer en tom oppgaveliste", e)
            emptyList()
        }
    }

    override fun hentNesteOppgaver(sakslisteId: Long): List<Oppgave> {
        return hentOppgaver(sakslisteId)
    }

    override fun hentOppgaverForSaksnummer(fagsakSaksnummer: Long): List<Oppgave> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hentAktiveOppgaverForSaksnummer(fagsakSaksnummerListe: Collection<Long>): List<Oppgave> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hentReservasjonerTilknyttetAktiveOppgaver(): List<Reservasjon> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun reserverOppgave(oppgaveId: Long): Reservasjon {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hentReservasjon(oppgaveId: Long): Reservasjon {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun frigiOppgave(oppgaveId: Long, begrunnelse: String): Reservasjon {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun forlengReservasjonPåOppgave(oppgaveId: Long): Reservasjon {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun flyttReservasjon(oppgaveId: Long, brukernavn: String, begrunnelse: String): Reservasjon {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hentAlleOppgaveFiltrering(brukerIdent: String): List<OppgaveKø> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hentOppgaveFiltreringerForPåloggetBruker(): List<OppgaveKø> {
        return hentAlleOppgaveFiltrering("K9LOS")
    }

    override fun hentPersonInfo(aktørId: Long): TpsPersonDto {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hentPersonInfoOptional(aktørId: Long): Optional<TpsPersonDto> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hentAntallOppgaver(behandlingsKø: Long, forAvdelingsleder: Boolean): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hentAntallOppgaverForAvdeling(avdelingEnhet: String): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun harForandretOppgaver(oppgaveIder: List<Long>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hentSakslistensSaksbehandlere(sakslisteId: Long): List<SaksbehandlerinformasjonDto> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hentSisteReserverteOppgaver(): List<OppgaveDto> {
        val reserverteOppgave = oppgaveRepository.hentReserverteOppgaver("alexaban")

        return reserverteOppgave.stream().map { t ->
            OppgaveDto(
                OppgaveStatusDto(
                    true, LocalDateTime.of(2020, 3, 25, 12, 45),
                    true, "alexaban", "Klara Saksbehandler", null
                ),
                21314,
                "6546765",
                "Walter Lemon",
                "VL",
                "453555245",
                BehandlingType.SOKNAD,
                FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
                BehandlingStatus.OPPRETTET,
                true,
                LocalDateTime.now(),
                LocalDateTime.of(2020, 7, 13, 12, 34),
                UUID.randomUUID()
            )
        }.toList()
    }

    override fun hentSaksbehandlerNavnOgAvdelinger(ident: String): SaksbehandlerinformasjonDto {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hentNavnHvisReservertAvAnnenSaksbehandler(reservasjon: Reservasjon): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hentNavnHvisFlyttetAvSaksbehandler(flyttetAv: String): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}