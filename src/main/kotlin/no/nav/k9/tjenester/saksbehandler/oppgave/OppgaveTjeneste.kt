package no.nav.k9.tjenester.saksbehandler.oppgave

//import no.nav.k9.integrasjon.K9SakRestKlient
import no.nav.k9.domene.lager.aktør.TpsPersonDto
import no.nav.k9.domene.lager.oppgave.Oppgave
import no.nav.k9.domene.lager.oppgave.OppgaveModell
import no.nav.k9.domene.lager.oppgave.Reservasjon
import no.nav.k9.domene.modell.OppgaveKø
import no.nav.k9.domene.repository.OppgaveKøRepository
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.integrasjon.pdl.PdlService
import no.nav.k9.integrasjon.rest.idToken
import no.nav.k9.tilgangskontroll.log
import no.nav.k9.tjenester.fagsak.FagsakDto
import no.nav.k9.tjenester.fagsak.PersonDto
import no.nav.k9.tjenester.saksbehandler.IdToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
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
            val alleOppgaver = oppgaveRepository.hentAktiveOppgaver().stream()
                .filter { t -> t.sisteOppgave().reservasjon?.reservertAv.isNullOrEmpty() }
                .map { t -> t.sisteOppgave() }.toList()
            alleOppgaver
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

 /*   fun alder(pnr: String): Int {
        val år = pnr.substring(4, 5).toInt()
        val måned = pnr.substring(2, 3).toInt()
        val dag = pnr.substring(0, 1).toInt()
        val nå = LocalDate.now()

        val finalÅr = if (år in 0..nå.year) "20$år" else "19$år"

    } */

    suspend fun søkFagsaker(query: String): List<FagsakDto> {
        val aktørId = pdlService.identifikator(query)
        if (aktørId != null) {
            val person = pdlService.person(aktørId.toString())
            if (person != null) {
                return oppgaveRepository.hentOppgaverMedAktorId(query).map {
                    FagsakDto(
                        it.sisteOppgave().fagsakSaksnummer,
                        PersonDto(
                            person.data.hentPerson.navn[0].forkortetNavn,
                            person.data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer,
                            person.data.hentPerson.kjoenn.kjoenn,
                            person.data.hentPerson.doedsfall!!.doedsdato
                        ),
                        it.sisteOppgave().fagsakYtelseType,
                        it.sisteOppgave().behandlingStatus,
                        it.sisteOppgave().behandlingOpprettet,
                        it.sisteOppgave().aktiv
                    )
                }
            }
        }
        val oppgaver = oppgaveRepository.hentOppgaverMedSaksnummer(query)
        return oppgaver.map {
            val person = pdlService.person(it.sisteOppgave().aktorId)!!
            FagsakDto(
                it.sisteOppgave().fagsakSaksnummer,
                PersonDto(
                    person.data.hentPerson.navn[0].forkortetNavn,
                    person.data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer,
                    person.data.hentPerson.kjoenn.kjoenn,
                    person.data.hentPerson.doedsfall!!.doedsdato
                ),
                it.sisteOppgave().fagsakYtelseType,
                it.sisteOppgave().behandlingStatus,
                it.sisteOppgave().behandlingOpprettet,
                it.sisteOppgave().aktiv
            )
        }
    }

    suspend fun reservertAvMeg(ident: String): Boolean {
        return IdToken(coroutineContext.idToken().value).ident.value == ident

    }

    suspend fun tilOppgaveDto(oppgaveModell: OppgaveModell): OppgaveDto {

        val reservasjon = oppgaveModell.sisteOppgave().reservasjon
        val oppgaveStatus = if(reservasjon == null) OppgaveStatusDto(false, null, false, null, null)
            else OppgaveStatusDto(true,reservasjon.reservertTil, reservertAvMeg(reservasjon.reservertAv), reservasjon.reservertAv, null)
        val person = pdlService.person(oppgaveModell.sisteOppgave().aktorId)!!
        return OppgaveDto(
            oppgaveStatus,
            oppgaveModell.sisteOppgave().behandlingId,
            oppgaveModell.sisteOppgave().fagsakSaksnummer,
            person.data.hentPerson.navn[0].forkortetNavn,
            oppgaveModell.sisteOppgave().system,
            person.data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer,
            oppgaveModell.sisteOppgave().behandlingType,
            oppgaveModell.sisteOppgave().fagsakYtelseType,
            oppgaveModell.sisteOppgave().behandlingStatus,
            true,
            oppgaveModell.sisteOppgave().behandlingOpprettet,
            oppgaveModell.sisteOppgave().behandlingsfrist,
            oppgaveModell.sisteOppgave().eksternId,
            oppgaveModell.sisteOppgave().tilBeslutter,
            oppgaveModell.sisteOppgave().utbetalingTilBruker,
            oppgaveModell.sisteOppgave().selvstendigFrilans,
            oppgaveModell.sisteOppgave().kombinert,
            oppgaveModell.sisteOppgave().søktGradering,
            oppgaveModell.sisteOppgave().registrerPapir
        )
    }


    suspend fun hentOppgaverFraListe(saksnummere: List<String>): List<OppgaveDto> {
        val oppgaver = saksnummere.map { oppgaveRepository.hentOppgaverMedSaksnummer(it) }
        val finalList = mutableListOf<OppgaveDto>()
        oppgaver.forEach {
            it.forEach { o -> finalList.add(tilOppgaveDto(o)) }
        }
        return finalList
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
        return oppgaveRepository.hentAktiveOppgaver().size
    }

    suspend fun hentSisteReserverteOppgaver(ident: String): List<OppgaveDto> {
        val reserverteOppgave = oppgaveRepository.hentReserverteOppgaver(ident)
        val list = mutableListOf<OppgaveDto>()

        for (oppgavemodell in reserverteOppgave) {
            val person = pdlService.person(oppgavemodell.sisteOppgave().aktorId)
            if (person == null) {
                flyttOppgaveTilVikafossen(oppgave = oppgavemodell.sisteOppgave())
                log.info("Ikke tilgang til bruker: ${oppgavemodell.sisteOppgave().aktorId}")
                continue
            }
            val status = if (ident == "alexaban") {
                OppgaveStatusDto(
                    true,
                    oppgavemodell.sisteOppgave().reservasjon?.reservertTil,
                    true,
                    oppgavemodell.sisteOppgave().reservasjon?.reservertAv,
                    null
                )
            } else {
                OppgaveStatusDto(
                    true,
                    oppgavemodell.sisteOppgave().reservasjon?.reservertTil,
                    true,
                    oppgavemodell.sisteOppgave().reservasjon?.reservertAv,
                    null
                )
            }

            list.add(
                OppgaveDto(
                    status,
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

    fun hentOppgaveKøer(): MutableList<OppgaveKø> {
        return oppgaveKøRepository.hent()
    }

    fun flyttOppgaveTilVikafossen(oppgave: Oppgave) {
        oppgaveRepository.lagre(oppgave.eksternId, f = { forrigeOppgave ->
            forrigeOppgave?.skjermet = true
            log.info("setter ${forrigeOppgave.toString()} til skjermet")
            forrigeOppgave!!
        })
    }
}
