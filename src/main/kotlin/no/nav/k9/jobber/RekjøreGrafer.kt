package no.nav.k9.jobber

import io.ktor.application.*
import io.ktor.util.*
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import no.nav.k9.domene.repository.*
import no.nav.k9.tjenester.avdelingsleder.nokkeltall.AlleOppgaverNyeOgFerdigstilte
import java.util.*
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis


fun Application.rekjørForGrafer(
    behandlingProsessEventRepository: BehandlingProsessEventRepository,
    statistikkRepository: StatistikkRepository
) {
    launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
        val alleEventerIder = behandlingProsessEventRepository.hentAlleEventerIder()
        statistikkRepository.truncateNyeOgFerdigstilte()
        for ((index, eventId) in alleEventerIder.withIndex()) {
            if (index % 1000 == 0) {
                log.info("""Ferdig med $index av ${alleEventerIder.size}""")
            }
            for (modell in behandlingProsessEventRepository.hent(UUID.fromString(eventId)).alleVersjoner()) {
                val oppgave = modell.oppgave()
                if (modell.starterSak()) {
                    if (oppgave.aktiv) {
                        statistikkRepository.lagre(
                            AlleOppgaverNyeOgFerdigstilte(
                                oppgave
                                    .fagsakYtelseType, oppgave.behandlingType, oppgave.eventTid.toLocalDate()
                            )
                        ) {
                            it.nye.add(oppgave.eksternId.toString())
                            it
                        }
                    }
                }
                if (modell.forrigeEvent() != null && !modell.oppgave(modell.forrigeEvent()!!).aktiv && modell.oppgave().aktiv) {
                    statistikkRepository.lagre(
                        AlleOppgaverNyeOgFerdigstilte(
                            oppgave.fagsakYtelseType,
                            oppgave.behandlingType,
                            oppgave.eventTid.toLocalDate()
                        )
                    ) {
                        it.nye.add(oppgave.eksternId.toString())
                        it
                    }
                }

                if (modell.forrigeEvent() != null && modell.oppgave(modell.forrigeEvent()!!).aktiv && !modell.oppgave().aktiv) {
                    statistikkRepository.lagre(
                        AlleOppgaverNyeOgFerdigstilte(
                            oppgave.fagsakYtelseType,
                            oppgave.behandlingType,
                            oppgave.eventTid.toLocalDate()
                        )
                    ) {
                        it.ferdigstilte.add(oppgave.eksternId.toString())
                        it
                    }
                }
            }
        }
    }
}


@KtorExperimentalAPI
 fun Application.regenererOppgaver(
    oppgaveRepository: OppgaveRepository,
    behandlingProsessEventRepository: BehandlingProsessEventRepository,
    reservasjonRepository: ReservasjonRepository,
    oppgaveKøRepository: OppgaveKøRepository,
    saksbehhandlerRepository: SaksbehandlerRepository
) {
    launch(context = Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
        try {

            log.info("Starter oppgavesynkronisering")
            val measureTimeMillis = measureTimeMillis {
                val hentAktiveOppgaver = oppgaveRepository.hentAktiveOppgaver()
                for ((index, aktivOppgave) in hentAktiveOppgaver.withIndex()) {
                    val event = behandlingProsessEventRepository.hent(aktivOppgave.eksternId)
                    val oppgave = event.oppgave()
                    if (!oppgave.aktiv) {
                        if (reservasjonRepository.finnes(oppgave.eksternId)) {
                            reservasjonRepository.lagre(oppgave.eksternId) { reservasjon ->
                                reservasjon!!.reservertTil = null
                                reservasjon
                            }
                            val reservasjon = reservasjonRepository.hent(oppgave.eksternId)
                            saksbehhandlerRepository.fjernReservasjonIkkeTaHensyn(
                                reservasjon.reservertAv,
                                reservasjon.oppgave
                            )
                        }
                    }
                    oppgaveRepository.lagre(oppgave.eksternId) {
                        oppgave
                    }
                    if (index % 10 == 0) {
                        log.info("Synkronisering " + index + " av " + hentAktiveOppgaver.size)
                    }
                }
                for (oppgavekø in oppgaveKøRepository.hentIkkeTaHensyn()) {
                    oppgaveKøRepository.oppdaterKøMedOppgaver(oppgavekø.id)
                }
            }
            for (oppgavekø in oppgaveKøRepository.hentIkkeTaHensyn()) {
                oppgaveKøRepository.oppdaterKøMedOppgaver(oppgavekø.id)
            }
            log.info("Avslutter oppgavesynkronisering: $measureTimeMillis ms")
        } catch (e: Exception) {
            log.error("", e)
        }
    }
}