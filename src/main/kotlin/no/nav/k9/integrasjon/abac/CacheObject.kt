package no.nav.k9.integrasjon.abac

import java.time.LocalDateTime

data class CacheObject( val value:Boolean,  val expire : LocalDateTime = LocalDateTime.now().plusMinutes(5)) {
}