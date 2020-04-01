package no.nav.k9.datavarehus


/**
 * Det overordnede behandlingsforløp knyttet til én bruker, referert til det samme rettighetsforholdet. Saken henviser til den overordnede «mappen» av behandlinger.
 */
data class Sak(
    /**
     * Aktør iden til primær mottag av ytelsen om denne blir godkjent. Altså, den som saken omhandler.
     */
    val aktorId: Double,
    val aktorer: Aktorer,
    /**
     * Felte angir hvem som er avsender av dataene, så navnet på kildesystemet.
     */
    val avsender: Boolean,
    /**
     * Tidspunktet da hendelsen faktisk ble gjennomført eller registrert i kildesystemet. (format:yyyy-mm-ddThh24:mn:ss.FF6) Dette er det tidspunkt der hendelsen faktisk er gjeldende fra. Ved for eksempel patching av data eller oppdatering tilbake i tid, skal teksniskTid være lik endrings tidspunktet, mens funksjonellTid angir tidspunktet da endringen offesielt gjelder fra.
     */
    val funksjonellTid: String,
    /**
     * Tidspunkt da saken først blir opprettet. Denne datoen forteller noe om når saken først oppstod hos oss. Dette kan være dato for mottatt brev, sakens første opprettelse ved digital søknad o.l.
     */
    val opprettetDato: String,
    /**
     * Kode som angir resultet av behandling på innværende tidspunkt. For eksempel, ikke fastsatt, innvilget, avslått.
     */
    val resultat: String,
    /**
     * Beskriver den funksjonelle verdien av koden. Finnes ikke den enda, regner vi med å få den senere.
     */
    val resultatBeskrivelse: String,
    /**
     * Nøkkelen til saken i kildesystemet. Noen kildesystem har valgt å kalle dette objektet fagsak. Denne identifiserer samlingen av behandlinger som vil kunne oppstå i forbindelse med saken. Skal kunne spores i kilden.
     */
    val sakId: Double,
    /**
     * Saksnummeret tilknyttet saken. Dette kan være det samme som sakId, om dette også gjelder kildesystemet.
     */
    val saksnummer: String,
    /**
     * Kode som angir sakens status, slik som påbegynt, under utbetaling, avsluttet o.l.
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
     * Noen kildesystemer vil ha en under type som gir finnere oppløsning på saken omhandler. For eksempel vil foreldrepenger være en ytelseType, i FPSAK, mens underType vil da angi hvilken type foreldrepnger sak det er snakk, altså Adopsjon, Fødsel o.l. Dette er sakens underType. Hvis dette ikke finnes eller allerede er bakt inn i ytelseType kan denne være tom.
     */
    val underType: String,
    /**
     * Beskriver den funksjonelle verdien av koden. Finnes ikke den enda, regner vi med å få den senere.
     */
    val underTypeBeskrivelse: String,
    /**
     * Angir på hvilken versjon av kilde koden objektet er JSON stringer er generert med bakgrunn på.
     */
    val versjon: Double,
    /**
     * Stønaden eller ytelsen det er saken omhandler. Hva gjelder saken?
     */
    val ytelseType: String,
    /**
     * Beskriver den funksjonelle verdien av koden. Finnes ikke den enda, regner vi med å få den senere.
     */
    val ytelseTypeBeskrivelse: String,
    /**
     * Denne datoen forteller fra hvilken dato saken først ble initiert. Datoen brukes i beregning av saksbehandlingstid og skal samsvare med brukerens opplevelse av at saksbehandlingen har startet.
     */
    val mottattDato: String
) {
    data class Aktorer(
        /**
         * Aktør iden til aktuelt person.
         */
        val aktorId: Double,
        /**
         * Kode som beskriver personens rolle i forbindelse med saken. Eksempelvis medmed, far samboer, barn o.l.
         */
        val rolle: String,
        /**
         * Beskriver den funksjonelle verdien av koden. Finnes ikke den enda, regner vi med å få den senere.
         */
        val rolleBeskrivelse: String
    )
}