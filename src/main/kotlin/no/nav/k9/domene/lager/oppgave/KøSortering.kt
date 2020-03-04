package no.nav.k9.domene.lager.oppgave

import com.fasterxml.jackson.annotation.JsonProperty

enum class KøSortering (): Kodeverdi {

    BEHANDLINGSFRIST("BEHFRIST", "Dato for behandlingsfrist", "", ""),
    OPPRETT_BEHANDLING("OPPRBEH", "Dato for opprettelse av behandling", "", ""),
    FORSTE_STONADSDAG("FORSTONAD", "Dato for første stønadsdag", "", ""),
    BELØP("BELOP", "Beløp", "HELTALL", "TILBAKEKREVING"),
    FEILUTBETALINGSTART("FEILUTBETALINGSTART", "Dato for første feilutbetaling", "DATO", "TILBAKEKREVING");

    override val kodeverk = "KO_SORTERING"
    val FT_HELTALL = "HELTALL"
    val FT_DATO = "DATO"

    val FK_UNIVERSAL = "UNIVERSAL"
    val FK_TILBAKEKREVING = "TILBAKEKREVING"

    @JsonProperty("kode")
    override var kode = ""
    @JsonProperty("navn")
    override var navn = ""
    @JsonProperty("felttype")
    var felttype: String? = null
    @JsonProperty("feltkategori")
    var feltkategori: String? = null

    constructor (kode: String?, navn: String?) {
        this.kode = kode!!
        this.navn = navn!!
        this.felttype = FT_DATO
        this.feltkategori = FK_UNIVERSAL
    }

    constructor(
        kode: String?,
        navn: String?,
        felttype: String,
        feltkategori: String
    ) {
        this.kode = kode!!
        this.navn = navn!!
        this.felttype = felttype
        this.feltkategori = feltkategori
    }

}
