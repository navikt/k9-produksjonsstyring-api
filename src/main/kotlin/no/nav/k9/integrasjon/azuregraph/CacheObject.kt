package no.nav.k9.integrasjon.azuregraph

import java.time.LocalDateTime

data class CacheObject( val value:String,  val expire : LocalDateTime = LocalDateTime.now().plusDays(1)) {
}