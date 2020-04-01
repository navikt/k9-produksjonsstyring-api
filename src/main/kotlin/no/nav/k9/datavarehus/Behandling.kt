package no.nav.k9.datavarehus


/**
 * Det overordnede behandlingsforløp knyttet til én bruker, referert til det samme rettighetsforholdet. Saken henviser til den overordnede «mappen» av behandlinger.
 */
data class Behandling(
    /**
     * Kode som angir hvilken enhet som er ansvarlig for behandlingen på den gjeldende tidspunktet.
     */
    val ansvarligEnhetKode: String,
    /**
     * Kode som angir hvilken type enhetskode det er snakk om, som oftest NORG.
     */
    val ansvarligEnhetType: String,
    /**
     * Felte angir hvem som er avsender av dataene, så navnet på kildesystemet.
     */
    val avsender: String,
    /**
     * Kode som angir hvilken enhet som faktisk utfører behandlingen på det gjeldende tidspunktet.
     */
    val behandlendeEnhetKode: String,
    /**
     * Kode som angir hvilken type enhetskode det er snakk om, som oftest NORG. Kan også angi en automatisk prosess.
     */
    val behandlendeEnhetType: String,
    /**
     * Nøkkel til den aktuelle behandling, som kan identifiserer den i kildensystemet.
     */
    val behandlingId: String,
    /**
     * Behandling opprettet manuelt av saksbehandler.
     */
    val behandlingManueltOpprettet: Boolean,
    /**
     * Opprinnelsen til behandlingen. Mulige verdier: AktørID, saksbehandlerID, system (automatisk)
     */
    val behandlingOpprettetAv: String,
    /**
     * Hvordan er behandlingen opprettet. Mulige verdier: Scannet papirskjema, digitalt skjema, automatisk
     */
    val behandlingOpprettetType: String,
    /**
     * Kode som beskriver behandlingsens utlandstilsnitt i henhold til NAV spesialisering. I hoved sak vil denne koden beskrive om saksbehandlingsfrister er i henhold til utlandssaker eller innlandssaker, men vil for mange kildesystem være angitt med en høyere oppløsning.
     */
    val behandlingOpprettetTypeBeskrivelse: String,
    /**
     * Kode som beskriver behandling, for eksempel, første gangsøknad, revurdering, klage o.l
     */
    val behandlingType: String,
    /**
     * Beskriver den funksjonelle verdien av koden. Finnes ikke den enda, regner vi med å få den senere.
     */
    val behandlingTypeBeskrivelse: String,
    /**
     * Bruker IDen til den ansvarlige beslutningstageren for saken.
     */
    val beslutter: String,
    /**
     * Den faktiske datoen for når stønaden/ytelsen betales ut til bruker.
     */
    val datoForUtbetaling: String,
    /**
     * Den forspeilte datoen for når stønade/ytelsen betales ut, bærer frutk for brukeren.
     */
    val datoForUttak: String,
    /**
     * Tidspunktet da hendelsen faktisk ble gjennomført eller registrert i kildesystemet. (format:yyyy-mm-ddThh24:mn:ss.FF6) Dette er det tidspunkt der hendelsen faktisk er gjeldende fra. Ved for eksempel patching av data eller oppdatering tilbake i tid, skal teksniskTid være lik endrings tidspunktet, mens funksjonellTid angir tidspunktet da endringen offesielt gjelder fra.
     */
    val funksjonellTid: String,
    /**
     * Denne datoen forteller fra hvilken dato behandlingen først ble initiert. Datoen brukes i beregning av saksbehandlingstid og skal samsvare med brukerens opplevelse av at saksbehandlingen har startet.
     */
    val mottattDato: String,
    /**
     * Tidspunkt for når behandlingen ble registrert i saksbehandlingssystemet. Denne kan avvike fra mottattDato hvis det tar tid fra postmottak til registrering i system, eller hvis en oppgave om å opprette behandling ligger på vent et sted i NAV. Ved automatisk registrering av saker er denne samme som mottattDato.
     */
    val registrertDato: String,
    /**
     * Hvis oppstår som resultat av en tidligere behandling, skal denne behandlingen referes til. Dette vil være tilfelle vis behandlingen er en revudring eller klage.
     */
    val relatertBehandlingId: String,
    /**
     * Kode som angir resultet av behandling på innværende tidspunkt. Mulige verdier: innvilget (delvis innvilget), avslått, omgjort, feilregistrert, henlagt, trukket, avvist etc.
     */
    val resultat: String,
    /**
     * Denne må inneholde en årsaksbeskrivelse knyttet til et hvert mulig resultat av behandlingen. Den kan enten være underordnet resultat eller stå for seg selv. Eks. årsak til avslag, årsak til delvis innvilgelse.
     */
    val resultatBegrunnelse: String,
    /**
     * Beskriver den funksjonelle verdien av koden. Finnes ikke den enda, regner vi med å få den senere.
     */
    val resultatBegrunnelseBeskrivelse: String,
    /**
     * Beskriver den funksjonelle verdien av koden. Finnes ikke den enda, regner vi med å få den senere.
     */
    val resultatBeskrivelse: String,
    /**
     * Nøkkelen til saken i kildesystemet. Noen kildesystem har valgt å kalle det fagsak. Denne identifiserer samlingen av behandlinger som vil kunne oppstå i forbindelse med saken. Skal kunne spores i kilden. Nøkkelen brukes til å se sammenhengen mellom ulike behandlinger og deres reise i NAV.
     */
    val sakId: String,
    /**
     * Bruker IDen til saksbehandler ansvarlig for saken på gjeldende tidspunkt. Kan etterlates tom ved helautomatiske delprosesser i behandlingen. Bør bare fylles når det er manuelle skritt i saksbehandlingen som utføres.
     */
    val saksbehandler: String,
    /**
     * Saksnummeret tilknyttet saken. Dette kan være det samme som sakId, om dette også gjelder kildesystemet.
     */
    val saksnummer: String,
    /**
     * Kode som angir den aktuelle behandlingens tilstand på gjeldende tidspunkt. Ha med alle mulige statuser som er naturlig for det enkelte system/ytelse. Som minimum, angi om saken har følgende status: Registrert, Klar for behandling, Venter på bruker, venter på ekstern (arbeidsgiver, lege etc.), venter på utland, Avsluttet.Her bør det også angis at saken er behandlet av beslutter, men sendt i retur for ny behandling.
     */
    val status: String,
    /**
     * Beskriver den funksjonelle verdien av koden. Finnes ikke den enda, regner vi med å få den senere.
     */
    val statusBeskrivelse: String,
    /**
     * Tidspunktet da kildesystemet ble klar over hendelsen. (format:yyyy-mm-ddThh24:mn:ss.FF6). Dette er tidspunkt hendelsen ble endret i dato systemet. Sammen med funksjonellTid, vil vi kunne holde rede på hva som er blitt rapportert tidligere og når det skjer endrigner tilbake i tid.
     */
    val tekniskTid: String,
    /**
     * Behandlingen krever totrinngsbehandling.
     */
    val totrinnsbehandling: Boolean,
    /**
     * Kode som beskriver behandlingsens utlandstilsnitt i henhold til NAV spesialisering. I hoved sak vil denne koden beskrive om saksbehandlingsfrister er i henhold til utlandssaker eller innlandssaker, men vil for mange kildesystem være angitt med en høyere oppløsning.
     */
    val utenlandstilsnitt: String,
    /**
     * Kode som beskriver behandlingsens utlandstilsnitt i henhold til NAV spesialisering. I hoved sak vil denne koden beskrive om saksbehandlingsfrister er i henhold til utlandssaker eller innlandssaker, men vil for mange kildesystem være angitt med en høyere oppløsning.
     */
    val utenlandstilsnittBeskrivelse: String,
    /**
     * Nøkkel til det aktuelle vedtaket da behandlingen blir tilknyttet et slikt. Vi skal helst kunne identifisere vedtaket i kildensystemet.
     */
    val vedtakId: Double,
    /**
     * Tidspunkt da vedtaket på behandlingen falt.
     */
    val vedtaksDato: String,
    /**
     * Angir på hvilken versjon av kilde koden objektet er JSON stringer er generert med bakgrunn på.
     */
    val versjon: Double
)