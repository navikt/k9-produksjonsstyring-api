package no.nav.vedtak.felles.integrasjon.kafka

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import no.nav.k9.kafka.dto.EventHendelse
import no.nav.k9.kafka.dto.Fagsystem
import java.time.LocalDateTime
import java.util.*

data class BehandlingProsessEventDto (

    /**
     * Ekstern id for behandlingen. Id benyttes til oppslag i fagsystem.
     * Benytt samme id for alle oppdateringer av aksjonspunkt/prosess innenfor samme behandling.
     */
    val eksternId: UUID,
    val fagsystem: Fagsystem,
    val saksnummer: String,
    val aktørId: String,

    val behandlingId: Long, // fjernes etter overgang til eksternId

    /**
     * Tidspunkt for hendelse lokalt for fagsystem.
     */
    @JsonSerialize(using = ToStringSerializer::class)
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    val eventTid: LocalDateTime?,
    val eventHendelse: EventHendelse,
    val behandlinStatus: String, // fjernes etter overgang til behandlingStatus
    val behandlingStatus: String?,
    val behandlingSteg: String,
    val behandlendeEnhet: String,

    /**
     * Ytelsestype i kodeform. Eksempel: FP
     */
    val ytelseTypeKode: String,

    /**
     * Behandlingstype i kodeform. Eksempel: BT-002
     */
    val behandlingTypeKode: String,

    /**
     * Tidspunkt behandling ble opprettet
     */
    @JsonSerialize(using = ToStringSerializer::class)
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    val opprettetBehandling: LocalDateTime,

    /**
     * Map av aksjonspunktkode og statuskode.
     */
    val aksjonspunktKoderMedStatusListe: Map<String, String>

)
