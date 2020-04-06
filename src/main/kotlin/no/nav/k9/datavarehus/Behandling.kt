package no.nav.k9.datavarehus


import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Behandling er en avgjørelse i en Sak, knyttet til en konkret behandlingstype (eks. søknad, revurdering, endring, klage).
 */
data class Behandling(
    /**
     * Kode som angir hvilken enhet som er ansvarlig for behandlingen på det gjeldende tidspunktet.
     */
    @JsonProperty("ansvarligEnhetKode")
    val ansvarligEnhetKode: String,
    /**
     * Kode som angir hvilken type enhetskode det er snakk om, som oftest NORG.
     */
    @JsonProperty("ansvarligEnhetType")
    val ansvarligEnhetType: String,
    /**
     * Feltet angir hvem som er avsender av dataene, så navnet på kildesystemet.
     */
    @JsonProperty("avsender")
    val avsender: String,
    /**
     * Kode som angir hvilken enhet som faktisk utfører behandlingen på det gjeldende tidspunktet.
     */
    @JsonProperty("behandlendeEnhetKode")
    val behandlendeEnhetKode: String,
    /**
     * Kode som angir hvilken type enhetskode det er snakk om, som oftest NORG. Kan også angi en automatisk prosess.
     */
    @JsonProperty("behandlendeEnhetType")
    val behandlendeEnhetType: String,
    /**
     * Nøkkel til den aktuelle behandling, som kan identifiserer den i kildensystemet.
     */
    @JsonProperty("behandlingId")
    val behandlingId: String,
    /**
     * Opprinnelsen til behandlingen. Mulige verdier: AktørID, saksbehandlerID, system (automatisk)
     */
    @JsonProperty("behandlingOpprettetAv")
    val behandlingOpprettetAv: String,
    /**
     * Beskriver den funksjonelle verdien av koden. Finnes ikke den enda, regner vi med å få den senere.
     */
    @JsonProperty("behandlingOpprettetType")
    val behandlingOpprettetType: String,
    /**
     * Kode som beskriver behandlingsens utlandstilsnitt i henhold til NAV spesialisering. I hoved sak vil denne koden beskrive om saksbehandlingsfrister er i henhold til utlandssaker eller innlandssaker, men vil for mange kildesystem være angitt med en høyere oppløsning.
     */
    @JsonProperty("behandlingOpprettetTypeBeskrivelse")
    val behandlingOpprettetTypeBeskrivelse: String,
    /**
     * Kode som angir den aktuelle behandlingens tilstand på gjeldende tidspunkt. Ha med alle mulige statuser som er naturlig for det enkelte system/ytelse. Som minimum, angi om saken har følgende status: Registrert, Klar for behandling, Venter på bruker, venter på ekstern (arbeidsgiver, lege etc.), venter på utland, Avsluttet.Her bør det også angis at saken er behandlet av beslutter, men sendt i retur for ny behandling.
     */
    @JsonProperty("behandlingStatus")
    val behandlingStatus: String,
    /**
     * Beskriver den funksjonelle verdien av koden. Finnes ikke den enda, regner vi med å få den senere.
     */
    @JsonProperty("behandlingStatusBeskrivelse")
    val behandlingStatusBeskrivelse: String,
    /**
     * Kode som beskriver behandlingen, for eksempel, søknad, revurdering, klage, anke, endring, gjenopptak, tilbakekreving o.l.
     */
    @JsonProperty("behandlingType")
    val behandlingType: String,
    /**
     * Beskriver den funksjonelle verdien av koden. Finnes ikke den enda, regner vi med å få den senere.
     */
    @JsonProperty("behandlingTypeBeskrivelse")
    val behandlingTypeBeskrivelse: String,
    /**
     * Bruker IDen til den ansvarlige beslutningstageren for saken.
     */
    @JsonProperty("beslutter")
    val beslutter: String,
    /**
     * Den faktiske datoen for når stønaden/ytelsen betales ut til bruker.
     */
    @JsonProperty("datoForUtbetaling")
    val datoForUtbetaling: String,
    /**
     *  Den forespeilede datoen for når stønaden/ytelsen betales ut, bærer frukter for brukeren. Eks. Foreldrepenger er uttaksdato første utbetaling etter at foreldrepengeperioden har startet, ved Pensjon er uttaksdato tidspunktet for første pensjonsutbetaling.
     */
    @JsonProperty("datoForUttak")
    val datoForUttak: String,
    /**
     * Tidspunktet da hendelsen faktisk ble gjennomført eller registrert i kildesystemet. (format:yyyy-mm-ddThh24:mn:ss.FF6) Dette er det tidspunkt der hendelsen faktisk er gjeldende fra. Ved for eksempel patching av data eller oppdatering tilbake i tid, skal tekniskTid være lik endrings tidspunktet, mens funksjonellTid angir tidspunktet da endringen offisielt gjelder fra.
     */
    @JsonProperty("funksjonellTid")
    val funksjonellTid: String,
    /**
     * Denne datoen forteller fra hvilken dato behandlingen først ble initiert. Datoen brukes i beregning av saksbehandlingstid og skal samsvare med brukerens opplevelse av at saksbehandlingen har startet.
     */
    @JsonProperty("mottattDato")
    val mottattDato: String,
    /**
     * Tidspunkt for når behandlingen ble registrert i saksbehandlingssystemet. Denne kan avvike fra mottattDato hvis det tar tid fra postmottak til registrering i system, eller hvis en oppgave om å opprette behandling ligger på vent et sted i NAV. Ved automatisk registrering av saker er denne samme som mottattDato.
     */
    @JsonProperty("registrertDato")
    val registrertDato: String,
    /**
     * Hvis behandlingen oppstår som resultat av en tidligere behandling, skal det refereres til denne behandlingen. Eksempel gjelder dette ved revurdering eller klage, hvor det skal vises til opprinnelig behandling med aktuelt vedtak.
     */
    @JsonProperty("relatertBehandlingId")
    val relatertBehandlingId: String,
    /**
     * Kode som angir resultat av behandling på innværende tidspunkt. Mulige verdier: innvilget (delvis innvilget), avslått, omgjort, feilregistrert, henlagt, trukket, avvist etc.
     */
    @JsonProperty("resultat")
    val resultat: String,
    /**
     * Denne må inneholde en årsaksbeskrivelse knyttet til et hvert mulig resultat av behandlingen. Den kan enten være underordnet resultat eller stå for seg selv. Eks. årsak til avslag, årsak til delvis innvilgelse.
     */
    @JsonProperty("resultatBegrunnelse")
    val resultatBegrunnelse: String,
    /**
     * Beskriver den funksjonelle verdien av koden. Finnes ikke den enda, regner vi med å få den senere.
     */
    @JsonProperty("resultatBegrunnelseBeskrivelse")
    val resultatBegrunnelseBeskrivelse: String,
    /**
     * Beskriver den funksjonelle verdien av koden. Finnes ikke den enda, regner vi med å få den senere.
     */
    @JsonProperty("resultatBeskrivelse")
    val resultatBeskrivelse: String,
    /**
     * Nøkkelen til saken i kildesystemet. Noen kildesystem har valgt å kalle det fagsak. Denne identifiserer samlingen av behandlinger som vil kunne oppstå i forbindelse med saken. Skal kunne spores i kilden.
     */
    @JsonProperty("sakId")
    val sakId: String,
    /**
     * Bruker IDen til saksbehandler ansvarlig for saken på gjeldende tidspunkt. Kan etterlates tom ved helautomatiske delprosesser i behandlingen. Bør bare fylles når det er manuelle skritt i saksbehandlingen som utføres.
     */
    @JsonProperty("saksbehandler")
    val saksbehandler: String,
    /**
     * Saksnummeret tilknyttet saken. Dette kan være det samme som sakId, om dette også gjelder kildesystemet.
     */
    @JsonProperty("saksnummer")
    val saksnummer: String,
    /**
     * Tidspunktet da kildesystemet ble klar over hendelsen. (format:yyyy-mm-ddThh24:mn:ss.FF6). Dette er tidspunkt hendelsen ble endret i dato systemet. Sammen med funksjonellTid, vil vi kunne holde rede på hva som er blitt rapportert tidligere og når det skjer endringer tilbake i tid.
     */
    @JsonProperty("tekniskTid")
    val tekniskTid: String,
    /**
     * Behandlingen krever totrinnsbehandling.
     */
    @JsonProperty("totrinnsbehandling")
    val totrinnsbehandling: Boolean,
    /**
     * Kode som beskriver behandlingens  utlandstilsnitt i henhold til NAV spesialisering. I hoved sak vil denne koden beskrive om saksbehandlingsfrister er i henhold til utlandssaker eller innlandssaker, men vil for mange kildesystem være angitt med en høyere oppløsning.
     */
    @JsonProperty("utenlandstilsnitt")
    val utenlandstilsnitt: String,
    /**
     * Beskriver den funksjonelle verdien av koden. Finnes ikke den enda, regner vi med å få den senere.
     */
    @JsonProperty("utenlandstilsnittBeskrivelse")
    val utenlandstilsnittBeskrivelse: String,
    /**
     * Nøkkel til det aktuelle vedtaket da behandlingen blir tilknyttet et slikt. Vi skal helst kunne identifisere vedtaket i kildensystemet.
     */
    @JsonProperty("vedtakId")
    val vedtakId: String,
    /**
     * Tidspunkt da vedtaket på behandlingen falt.
     */
    @JsonProperty("vedtaksDato")
    val vedtaksDato: String,
    /**
     * Angir på hvilken versjon av kildekoden JSON stringen er generert på bakgrunn av.
     */
    @JsonProperty("versjon")
    val versjon: Double
)