package no.nav.k9.tjenester.saksbehandler.oppgave

//import no.nav.k9.integrasjon.K9SakRestKlient
import no.nav.k9.domene.lager.aktør.TpsPersonDto
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.lager.oppgave.Reservasjon
import no.nav.k9.domene.modell.OppgaveKø
import no.nav.k9.domene.oppslag.Attributt
import no.nav.k9.domene.oppslag.Ident
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.domene.typer.AktørId
import no.nav.k9.domene.typer.PersonIdent
import no.nav.k9.integrasjon.tps.TpsProxyV1Gateway
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import kotlin.streams.toList


private val LOGGER: Logger =
    LoggerFactory.getLogger(OppgaveTjenesteImpl::class.java)


class OppgaveTjenesteImpl(
    private val oppgaveRepository: OppgaveRepository,
    private val oppgaveKøRepository: OppgaveKøRepository,
    private val tpsProxyV1Gateway: TpsProxyV1Gateway
) {

    fun hentOppgaver(oppgavekøId: UUID): List<Oppgave> {
        return try {
            oppgaveRepository.hent().stream().filter { t -> t.sisteOppgave().reservasjon?.reservertAv.isNullOrEmpty() }
                .map { t -> t.sisteOppgave() }.toList()
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

    suspend fun hentPersonInfo(aktørId: Long): TpsPersonDto {
        val tpsPerson = tpsProxyV1Gateway.person(
            ident = Ident(aktørId.toString()),
            attributter = setOf(
                Attributt.fornavn,
                Attributt.mellomnavn,
                Attributt.etternavn,
                Attributt.diskresjonskode,
                Attributt.egenansatt,
                Attributt.kjønn,
                Attributt.ident
            )
        )
        val person = tpsPerson!!
        return TpsPersonDto(
            aktørId = AktørId(aktørId.toString()),
            diskresjonskode = person.diskresjonskode,
            fødselsdato = person.fødselsdato,
            fnr = PersonIdent(person.ident),
            kjønn = person.kjønn,
            dødsdato = person.dødsdato,
            navn = person.navn
        )
    }

    fun hentPersonInfoOptional(aktørId: Long): Optional<TpsPersonDto> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun hentAntallOppgaver(oppgavekøId: UUID, forAvdelingsleder: Boolean): Int {
        return hentOppgaver(oppgavekøId).size
    }

    fun hentAntallOppgaverForAvdeling(avdelingEnhet: String): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun hentSisteReserverteOppgaver(ident: String): List<OppgaveDto> {
        val reserverteOppgave = oppgaveRepository.hentReserverteOppgaver(ident)

        return reserverteOppgave.stream().map { t ->
            OppgaveDto(
                OppgaveStatusDto(
                    true, t.sisteOppgave().reservasjon?.reservertTil,
                    true, t.sisteOppgave().reservasjon?.reservertAv, "Klara Saksbehandler", null
                ),
                t.sisteOppgave().behandlingId,
                t.sisteOppgave().fagsakSaksnummer,
                "Walter Lemon",
                      t.sisteOppgave().system,
                      "453555245",
                      t.sisteOppgave().behandlingType,
                      t.sisteOppgave().fagsakYtelseType,
                      t.sisteOppgave().behandlingStatus,
                      true,
                      t.sisteOppgave().behandlingOpprettet,
                      t.sisteOppgave().behandlingsfrist,
                      t.sisteOppgave().eksternId
                  )
              }.toList()
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
