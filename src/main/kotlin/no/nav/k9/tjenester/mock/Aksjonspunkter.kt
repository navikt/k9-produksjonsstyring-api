package no.nav.k9.tjenester.mock

class Aksjonspunkter {
    fun aksjonspunkter(): List<Aksjonspunkt> {

        return listOf(
            Aksjonspunkt(
                kode = "5009",
                navn = "Avklar tilleggsopplysninger",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Kontroller Fakta",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5015",
                navn = "Vurder om ytelse allerede er innvilget",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Foreslå vedtak",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5016",
                navn = "Fatter vedtak",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Fatte Vedtak",
                plassering = "INN",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5017",
                navn = "Vurder søkers opplysningsplikt ved ufullstendig/ikke-komplett søknad",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Kontrollerer søkers opplysningsplikt",
                plassering = "UT",
                vilkårtype = "Søkers opplysningsplikt",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5018",
                navn = "Foreslå vedtak uten totrinnskontroll",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Foreslå vedtak",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5019",
                navn = "Avklar lovlig opphold.",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Vurder medlemskapvilkår",
                plassering = "INN",
                vilkårtype = "Medlemskapsvilkåret",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5020",
                navn = "Avklar om bruker er bosatt.",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Vurder medlemskapvilkår",
                plassering = "INN",
                vilkårtype = "Medlemskapsvilkåret",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5021",
                navn = "Avklar om bruker har gyldig periode.",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Vurder medlemskapvilkår",
                plassering = "INN",
                vilkårtype = "Medlemskapsvilkåret",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5022",
                navn = "Avklar fakta for status på person.",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Kontroller Fakta",
                plassering = "INN",
                vilkårtype = "Medlemskapsvilkåret",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5023",
                navn = "Avklar oppholdsrett.",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Vurder medlemskapvilkår",
                plassering = "INN",
                vilkårtype = "Medlemskapsvilkåret",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5026",
                navn = "Varsel om revurdering opprettet manuelt",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Varsel om revurdering",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5028",
                navn = "Foreslå vedtak manuelt",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Foreslå vedtak",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5030",
                navn = "Avklar verge",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Kontroller Fakta",
                plassering = "INN",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5031",
                navn = "Vurdere om søkers ytelse gjelder samme barn",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Vurder søkers relasjon til barnet",
                plassering = "UT",
                vilkårtype = "",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "5033",
                navn = "Vurdere annen ytelse før vedtak",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Foreslå vedtak",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5034",
                navn = "Vurdere dokument før vedtak",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Foreslå vedtak",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5038",
                navn = "Fastsette beregningsgrunnlag for arbeidstaker/frilanser skjønnsmessig",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Foreslå beregningsgrunnlag",
                plassering = "UT",
                vilkårtype = "Beregning",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "5039",
                navn = "Vurder varig endret/nyoppstartet næring selvstendig næringsdrivende",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Foreslå beregningsgrunnlag",
                plassering = "UT",
                vilkårtype = "Beregning",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "5042",
                navn = "Fastsett beregningsgrunnlag for selvstendig næringsdrivende",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Foreslå beregningsgrunnlag",
                plassering = "UT",
                vilkårtype = "Beregning",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "5046",
                navn = "Fordel beregningsgrunnlag",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Fordel beregningsgrunnlag",
                plassering = "UT",
                vilkårtype = "",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "5047",
                navn = "Fastsett beregningsgrunnlag for tidsbegrenset arbeidsforhold",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Foreslå beregningsgrunnlag",
                plassering = "UT",
                vilkårtype = "Beregning",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "5049",
                navn = "Fastsett beregningsgrunnlag for SN som er ny i arbeidslivet",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Foreslå beregningsgrunnlag",
                plassering = "UT",
                vilkårtype = "",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "5050",
                navn = "Vurder gradering på andel uten beregningsgrunnlag",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Fastsett beregningsgrunnlag",
                plassering = "UT",
                vilkårtype = "",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "5051",
                navn = "Vurder perioder med opptjening",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Vurder opptjeningsvilkåret",
                plassering = "INN",
                vilkårtype = "Opptjeningsvilkåret",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5052",
                navn = "Avklar aktivitet for beregning",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Fastsett skjæringstidspunkt beregning",
                plassering = "UT",
                vilkårtype = "",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "5053",
                navn = "Avklar fortsatt medlemskap.",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Kontroller løpende medlemskap",
                plassering = "UT",
                vilkårtype = "Medlemskapsvilkåret",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "5055",
                navn = "Vurder varsel ved vedtak til ugunst",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Foreslå vedtak",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5056",
                navn = "Kontroll av manuelt opprettet revurderingsbehandling",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Foreslå vedtak",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5058",
                navn = "Vurder fakta for arbeidstaker, frilans og selvstendig næringsdrivende",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Kontroller fakta for beregning",
                plassering = "UT",
                vilkårtype = "",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "5068",
                navn = "Innhent dokumentasjon fra utenlandsk trygdemyndighet",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Vurder kompletthet",
                plassering = "INN",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5072",
                navn = "Søker er stortingsrepresentant/administrativt ansatt i Stortinget",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Vurder uttaksvilkår",
                plassering = "UT",
                vilkårtype = "",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "5074",
                navn = "Kontroller opplysninger om medlemskap",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Vurder uttaksvilkår",
                plassering = "UT",
                vilkårtype = "",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "5076",
                navn = "Kontroller opplysninger om død",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Vurder uttaksvilkår",
                plassering = "UT",
                vilkårtype = "",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "5077",
                navn = "Kontroller opplysninger om søknadsfrist",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Vurder uttaksvilkår",
                plassering = "UT",
                vilkårtype = "",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "5078",
                navn = "Kontroller tilstøtende ytelser innvilget",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Vurder uttaksvilkår",
                plassering = "UT",
                vilkårtype = "",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "5079",
                navn = "Kontroller tilstøtende ytelser opphørt",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Vurder uttaksvilkår",
                plassering = "UT",
                vilkårtype = "",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "5080",
                navn = "Avklar arbeidsforhold",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Kontroller arbeidsforhold",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5084",
                navn = "Vurder feilutbetaling",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Simuler oppdrag",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5085",
                navn = "Vurder inntrekk",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Simuler oppdrag",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "5089",
                navn = "Manuell vurdering av opptjeningsvilkår",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Vurder opptjeningsvilkåret",
                plassering = "UT",
                vilkårtype = "Opptjeningsvilkåret",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "5090",
                navn = "Vurder tilbaketrekk",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Vurder tilbaketrekk",
                plassering = "UT",
                vilkårtype = "",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "5095",
                navn = "Vurder Faresignaler",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Vurder faresignaler",
                plassering = "UT",
                vilkårtype = "",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "9001",
                navn = "Vurder Faresignaler",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Vurder medisinskvilkår",
                plassering = "INN",
                vilkårtype = "Medisinskevilkår",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "6002",
                navn = "Saksbehandler initierer kontroll av søkers opplysningsplikt",
                aksjonspunktype = "Saksbehandleroverstyring",
                behandlingsstegtype = "Kontrollerer søkers opplysningsplikt",
                plassering = "UT",
                vilkårtype = "Søkers opplysningsplikt",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "6005",
                navn = "Overstyring av medlemskapsvilkåret",
                aksjonspunktype = "Overstyring",
                behandlingsstegtype = "Vurder medlemskapvilkår",
                plassering = "UT",
                vilkårtype = "Medlemskapsvilkåret",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "6007",
                navn = "Overstyring av beregning",
                aksjonspunktype = "Overstyring",
                behandlingsstegtype = "Beregn ytelse",
                plassering = "UT",
                vilkårtype = "",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "6011",
                navn = "Overstyring av opptjeningsvilkåret",
                aksjonspunktype = "Overstyring",
                behandlingsstegtype = "Vurder opptjeningsvilkåret",
                plassering = "UT",
                vilkårtype = "Opptjeningsvilkåret",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "6012",
                navn = "Overstyring av løpende medlemskapsvilkåret",
                aksjonspunktype = "Overstyring",
                behandlingsstegtype = "Vurder løpende medlemskap",
                plassering = "UT",
                vilkårtype = "Løpende medlemskapsvilkår",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "6014",
                navn = "Overstyring av beregningsaktiviteter",
                aksjonspunktype = "Overstyring",
                behandlingsstegtype = "Fastsett skjæringstidspunkt beregning",
                plassering = "UT",
                vilkårtype = "",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "6015",
                navn = "Overstyring av beregningsgrunnlag",
                aksjonspunktype = "Overstyring",
                behandlingsstegtype = "Kontroller fakta for beregning",
                plassering = "UT",
                vilkårtype = "",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "6068",
                navn = "Manuell markering av utenlandssak",
                aksjonspunktype = "Manuell",
                behandlingsstegtype = "Vurder kompletthet",
                plassering = "UT",
                vilkårtype = "",
                totrinn = true
            ),
            Aksjonspunkt(
                kode = "7001",
                navn = "Manuelt satt på vent",
                aksjonspunktype = "Autopunkt",
                behandlingsstegtype = "Kontroller Fakta",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "7003",
                navn = "Venter på komplett søknad",
                aksjonspunktype = "Autopunkt",
                behandlingsstegtype = "Vurder kompletthet",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "7005",
                navn = "Satt på vent etter varsel om revurdering",
                aksjonspunktype = "Autopunkt",
                behandlingsstegtype = "Varsel om revurdering",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "7006",
                navn = "Venter på opptjeningsopplysninger",
                aksjonspunktype = "Autopunkt",
                behandlingsstegtype = "Vurder opptjeningsvilkåret",
                plassering = "UT",
                vilkårtype = "Opptjeningsvilkåret",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "7007",
                navn = "Venter på scanning",
                aksjonspunktype = "Autopunkt",
                behandlingsstegtype = "Vurder innsynskrav",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "7008",
                navn = "Satt på vent pga for tidlig søknad",
                aksjonspunktype = "Autopunkt",
                behandlingsstegtype = "Vurder kompletthet",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "7009",
                navn = "Vent på oppdatering som passerer kompletthetssjekk",
                aksjonspunktype = "Autopunkt",
                behandlingsstegtype = "Fatte Vedtak",
                plassering = "INN",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "7013",
                navn = "Venter på søknad",
                aksjonspunktype = "Autopunkt",
                behandlingsstegtype = "Registrer søknad",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "7014",
                navn = "Vent på rapporteringsfrist for inntekt",
                aksjonspunktype = "Autopunkt",
                behandlingsstegtype = "Fastsett skjæringstidspunkt beregning",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "7019",
                navn = "Autopunkt gradering uten beregningsgrunnlag",
                aksjonspunktype = "Autopunkt",
                behandlingsstegtype = "Fastsett beregningsgrunnlag",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "7020",
                navn = "Vent på siste meldekort for AAP eller DP-mottaker",
                aksjonspunktype = "Autopunkt",
                behandlingsstegtype = "Fastsett skjæringstidspunkt beregning",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "7022",
                navn = "Vent på ny inntektsmelding med gyldig arbeidsforholdId",
                aksjonspunktype = "Autopunkt",
                behandlingsstegtype = "Kontroller arbeidsforhold",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "7023",
                navn = "Autopunkt militær i opptjeningsperioden og beregninggrunnlag under 3G",
                aksjonspunktype = "Autopunkt",
                behandlingsstegtype = "Fordel beregningsgrunnlag",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "7025",
                navn = "Autopunkt gradering flere arbeidsforhold",
                aksjonspunktype = "Autopunkt",
                behandlingsstegtype = "Fastsett beregningsgrunnlag",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            ),
            Aksjonspunkt(
                kode = "7030",
                navn = "Vent på etterlyst inntektsmelding",
                aksjonspunktype = "Autopunkt",
                behandlingsstegtype = "Innhent registeropplysninger - resterende oppgaver",
                plassering = "UT",
                vilkårtype = "",
                totrinn = false
            )
        )


    }
}

data class Aksjonspunkt(
    val kode: String,
    val navn: String,
    val aksjonspunktype: String,
    val behandlingsstegtype: String,
    val plassering: String,
    val vilkårtype: String,
    val totrinn: Boolean
)