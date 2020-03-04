package no.nav.k9.repository

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.k9.db.runMigration
import no.nav.k9.domene.lager.oppgave.*
import no.nav.k9.domene.repository.OppgaveRepository
import no.nav.k9.tjenester.saksbehandler.oppgave.OppgaveTjenesteImpl
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertTrue

class OppgaveRepositoryTest {

    @Test
    fun `Skal deserialisere`() {
//        val pg = EmbeddedPostgres.start()
//        val dataSource = pg.postgresDatabase
//        runMigration(dataSource)
//        val oppgaveRepository = OppgaveRepository(dataSource = dataSource)
//        val oppgaveTjeneste = OppgaveTjenesteImpl(oppgaveRepository = oppgaveRepository)
//
//        oppgaveTjeneste.oprettOppgave(
//            Oppgave(9877, "54798459", "78437843","ljkwre", LocalDateTime.now(), LocalDateTime.now(),
//                LocalDate.now(), BehandlingStatus.AVSLUTTET, BehandlingType.KLAGE, FagsakYtelseType.FORELDREPENGER, false, "blabalb", null, false, UUID.randomUUID(),
//                null, listOf(OppgaveEgenskap(324, AndreKriterierType.UTLANDSSAK, "jkdhe", false)), false, Aksjonspunkter(
//                    emptyMap())
//            ))
//        val oppgaver = oppgaveRepository.hentAlleOppgaver()
//
//        assertTrue { oppgaver.isNotEmpty() }
//        assertTrue { oppgaver[0].behandlingType.navn == "Klage" }
    }
}