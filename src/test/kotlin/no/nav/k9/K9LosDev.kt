package no.nav.k9

import com.github.tomakehurst.wiremock.WireMockServer
import io.ktor.server.testing.withApplication
import no.nav.helse.dusseldorf.testsupport.asArguments
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class K9LosDev {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(K9LosDev::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            val testArgs = TestConfiguration.asMap()
            withApplication { no.nav.k9.main(testArgs.asArguments()) }
        }
    }
}