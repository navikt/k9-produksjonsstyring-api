package no.nav.k9.domene.lager.oppgave

import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.k9.datavarehus.Behandling
import no.nav.k9.datavarehus.Sak
import no.nav.k9.domene.modell.Aksjonspunkter
import no.nav.k9.domene.modell.BehandlingType
import no.nav.k9.domene.modell.FagsakYtelseType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

data class Oppgave(
    val behandlingId: Long,
    val fagsakSaksnummer: String,
    val aktorId: String,
    val behandlendeEnhet: String,
    val behandlingsfrist: LocalDateTime,
    val behandlingOpprettet: LocalDateTime,
    val forsteStonadsdag: LocalDate,
    var behandlingStatus: BehandlingStatus,
    val behandlingType: BehandlingType,
    val fagsakYtelseType: FagsakYtelseType,
    val aktiv: Boolean,
    val system: String,
    val oppgaveAvsluttet: LocalDateTime?,
    val utfortFraAdmin: Boolean,
    val eksternId: UUID,
    var reservasjon: Reservasjon?,
    val oppgaveEgenskap: List<OppgaveEgenskap>,
    val beslutterOppgave: Boolean,
    val aksjonspunkter: Aksjonspunkter


) {
    fun datavarehusSak(): String {
        val sak = Sak(
            aktorId = 0.0,
            aktorer = Sak.Aktorer(aktorId = aktorId.toLong(), rolle = "Søken", rolleBeskrivelse = ""),
            avsender = false,
            funksjonellTid = this.behandlingOpprettet.format(DateTimeFormatter.ISO_DATE_TIME),
            opprettetDato = "",
            resultat = "",
            resultatBeskrivelse = "",
            sakId = 0L,
            saksnummer = fagsakSaksnummer,
            status = behandlingStatus.toString(),
            statusBeskrivelse = "",
            tekniskTid = "",
            underType = "",
            underTypeBeskrivelse = "",
            versjon = 0.0,
            ytelseType = fagsakYtelseType.navn,
            ytelseTypeBeskrivelse = "",
            mottattDato = ""
        )
        return objectMapper().writeValueAsString(sak)!!
    }

    fun datavarehusBehandling(): String {
        // lag statistikk

        val beslutter = if (beslutterOppgave) {
            reservasjon?.reservertAv ?: ""
        } else {
            ""
        }
        val behandling = Behandling(
            ansvarligEnhetKode = behandlendeEnhet,
            ansvarligEnhetType = "NORG",
            avsender = "K9SAK",
            behandlendeEnhetKode = behandlendeEnhet,
            behandlendeEnhetType = "NORG",
            behandlingId = behandlingId.toString(),
            behandlingManueltOpprettet = false,
            behandlingOpprettetAv = "system",
            behandlingOpprettetType = "automatisk",
            behandlingOpprettetTypeBeskrivelse = "Opprettet automatisk av systemet",
            behandlingType = behandlingType.navn,
            behandlingTypeBeskrivelse = "",
            beslutter = beslutter,
            datoForUtbetaling = forsteStonadsdag.format(DateTimeFormatter.BASIC_ISO_DATE),
            datoForUttak = "",
            funksjonellTid = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
            mottattDato = behandlingOpprettet.format(DateTimeFormatter.ISO_DATE_TIME),
            registrertDato = behandlingOpprettet.format(DateTimeFormatter.ISO_DATE_TIME),
            relatertBehandlingId = "",
            resultat = "",
            resultatBegrunnelse = "",
            resultatBegrunnelseBeskrivelse = "",
            resultatBeskrivelse = "",
            sakId = "",
            saksbehandler = "",
            saksnummer = fagsakSaksnummer,
            status = behandlingStatus.name,
            statusBeskrivelse = "",
            tekniskTid = "",
            totrinnsbehandling = beslutterOppgave,
            utenlandstilsnitt = "",
            utenlandstilsnittBeskrivelse = "",
            vedtakId = 0.0,
            vedtaksDato = "",
            versjon = 0.0
        )

        return objectMapper().writeValueAsString(behandling)!!
    }
}