package no.nav.k9.integrasjon.pdl

import java.time.LocalDateTime

data class CacheObject( val value:String,  val expire : LocalDateTime = LocalDateTime.now().plusMinutes(60)) {
}