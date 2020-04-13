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
            oppgaveRepository.hent().stream().map { t -> t.sisteOppgave() }.toList()
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
        val reservasjon: Reservasjon? = null
        oppgaveRepository.lagre(uuid) { forrigeOppgave ->
            forrigeOppgave?.reservasjon?.reservertTil = forrigeOppgave?.reservasjon?.reservertTil?.plusHours(24)
            forrigeOppgave!!
        }

        return reservasjon!!
    }

    fun flyttReservasjon(uuid: UUID, ident: String, begrunnelse: String): Reservasjon {
        val reservasjon: Reservasjon? = null
        oppgaveRepository.lagre(uuid) { forrigeOppgave ->
            forrigeOppgave?.reservasjon?.reservertTil = forrigeOppgave?.reservasjon?.reservertTil?.plusHours(24)
            forrigeOppgave?.reservasjon?.flyttetTidspunkt = LocalDateTime.now()
            forrigeOppgave?.reservasjon?.reservertAv = ident
            forrigeOppgave?.reservasjon?.begrunnelse = begrunnelse
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

    fun hentSisteReserverteOppgaver(): List<OppgaveDto> {
        val reserverteOppgave = oppgaveRepository.hentReserverteOppgaver("alexaban")

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
//        return listOf(
//            OppgaveDto(
//                OppgaveStatusDto(
//                    true, LocalDateTime.of(2020, 10, 1, 12, 13), true,
//                    "4fe", "Sara", null
//                ),
//                45323,
//                "9080800900",
//                "Lemo Water",
//                "ee09",
//                "23090382974",
//                BehandlingType.SOKNAD,
//                FagsakYtelseType.PLEIEPENGER_SYKT_BARN,
//                BehandlingStatus.OPPRETTET,
//                true,
//                LocalDateTime.of(2020, 3, 15, 13, 15),
//                LocalDateTime.of(2020, 9, 23, 12, 0),
//                UUID.randomUUID()
//            )
//        )
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
