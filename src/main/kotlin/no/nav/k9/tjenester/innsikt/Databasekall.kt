package no.nav.k9.tjenester.innsikt

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder

class Databasekall (){
    companion object{
        val map = ConcurrentHashMap<String, LongAdder>()
    }
    
}