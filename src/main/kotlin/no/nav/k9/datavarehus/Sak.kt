package no.nav.k9.datavarehus

import com.fasterxml.jackson.annotation.JsonProperty


/**
 * Det overordnede behandlingsforløp knyttet til én bruker, referert til det samme rettighetsforholdet. Saken henviser til den overordnede «mappen» av behandlinger.
 */
data class Sak(
    /**
     * Aktør IDen til primær mottager av ytelsen om denne blir godkjent. Altså, den som saken omhandler.
     */
    @JsonProperty("aktorId")
    val aktorId: Double,
    @JsonProperty("aktorer")
    val aktorer: List<Aktorer>,
    /**
     * Feltet angir hvem som er avsender av dataene, så navnet på kildesystemet.
     */
    @JsonProperty("avsender")
    val avsender: String,
    /**
     * Tidspunktet da hendelsen faktisk ble gjennomført eller registrert i kildesystemet. (format:yyyy-mm-ddThh24:mn:ss.FF6) Dette er det tidspunkt der hendelsen faktisk er gjeldende fra. Ved for eksempel patching av data eller oppdatering tilbake i tid, skal tekniskTid være lik endringstidspunktet, mens funksjonellTid angir tidspunktet da endringen offisielt gjelder fra.
     */
    @JsonProperty("funksjonellTid")
    val funksjonellTid: String,
    /**
     * Tidspunkt da saken først blir opprettet. Denne datoen forteller noe om når saken først oppstod hos oss. Dette kan være dato for mottatt brev, sakens første opprettelse ved digital søknad o.l.
     */
    @JsonProperty("opprettetDato")
    val opprettetDato: String,
    /**
     * Beskriver den funksjonelle verdien av koden. Finnes ikke den enda, regner vi med å få den senere.
     */
    @JsonProperty("resultatBeskrivelse")
    val resultatBeskrivelse: String,
    /**
     * Nøkkelen til saken i kildesystemet. Noen kildesystem har valgt å kalle dette objektet fagsak. Denne identifiserer samlingen av behandlinger som vil kunne oppstå i forbindelse med saken. Skal kunne spores i kilden.
     */
    @JsonProperty("sakId")
    val sakId: String,
    /**
     * Kode som angir sakens status, slik som påbegynt, under utbetaling, avsluttet o.l.
     */
    @JsonProperty("sakStatus")
    val sakStatus: String,
    /**
     * Beskriver den funksjonelle verdien av koden. Finnes ikke den enda, regner vi med å få den senere.
     */
    @JsonProperty("sakStatusBeskrivelse")
    val sakStatusBeskrivelse: String,
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
     * Noen kildesystemer vil ha en under-type som gir finere oppløsning på hva saken omhandler. For eksempel vil foreldrepenger være en ytelseType, i FPSAK, mens underType vil da angi hva slags type foreldrepenge-sak det er snakk, som Adopsjon, Fødsel o.l. Dette er sakens underType. Hvis dette ikke finnes eller allerede er bakt inn i ytelseType kan denne være tom.
     */
    @JsonProperty("underType")
    val underType: String,
    /**
     * Beskriver den funksjonelle verdien av koden. Finnes ikke den enda, regner vi med å få den senere.
     */
    @JsonProperty("underTypeBeskrivelse")
    val underTypeBeskrivelse: String,
    /**
     * Angir på hvilken versjon av kildekoden JSON stringen er generert på bakgrunn av.
     */
    @JsonProperty("versjon")
    val versjon: Double,
    /**
     * Stønaden eller ytelsen det er saken omhandler. Hva gjelder saken?
     */
    @JsonProperty("ytelseType")
    val ytelseType: String,
    /**
     * Beskriver den funksjonelle verdien av koden. Finnes ikke den enda, regner vi med å få den senere.
     */
    @JsonProperty("ytelseTypeBeskrivelse")
    val ytelseTypeBeskrivelse: String
) {
    data class Aktorer(
        /**
         * Aktør IDen til aktuell person.
         */
        @JsonProperty("aktorId")
        val aktorId: Long,
        /**
         * Kode som beskriver personens rolle i forbindelse med saken. Eksempelvis medmor, medfar, far samboer, barn o.l.
         */
        @JsonProperty("rolle")
        val rolle: String,
        /**
         * Beskriver den funksjonelle verdien av koden. Finnes ikke den enda, regner vi med å få den senere.
         */
        @JsonProperty("rolleBeskrivelse")
        val rolleBeskrivelse: String
    )
}