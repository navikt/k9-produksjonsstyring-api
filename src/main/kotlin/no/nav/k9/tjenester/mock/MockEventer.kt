package no.nav.k9.tjenester.mock

class MockEventer() {
    fun startBehandling() {
        //language=JSON
        """{
              "eksternId": "bc0636ee-b26c-4155-a787-95a82392944c",
              "fagsystem": "K9SAK",
              "saksnummer": "5YC4K",
              "aktørId": "9929038413668",
              "behandlingId": 1000002,
              "eventTid": "2020-03-16T10:38:03.017816",
              "eventHendelse": "BEHANDLINGSKONTROLL_EVENT",
              "behandlinStatus": "UTRED",
              "behandlingStatus": null,
              "behandlingSteg": "INREG",
              "behandlendeEnhet": "4833",
              "ytelseTypeKode": "PSB",
              "behandlingTypeKode": "BT-002",
              "opprettetBehandling": "2020-03-16T10:38",
              "aksjonspunktKoderMedStatusListe": {}
            }
          """

    }

    fun ventPåEtterlystInntektsmelding() {
        //language=JSON
        """{
              "eksternId": "bc0636ee-b26c-4155-a787-95a82392944c",
              "fagsystem": "K9SAK",
              "saksnummer": "5YC4K",
              "aktørId": "9929038413668",
              "behandlingId": 1000002,
              "eventTid": "2020-03-16T10:38:07.439524",
              "eventHendelse": "BEHANDLINGSKONTROLL_EVENT",
              "behandlinStatus": "UTRED",
              "behandlingStatus": null,
              "behandlingSteg": "INREG_AVSL",
              "behandlendeEnhet": "4833",
              "ytelseTypeKode": "PSB",
              "behandlingTypeKode": "BT-002",
              "opprettetBehandling": "2020-03-16T10:38",
              "aksjonspunktKoderMedStatusListe": {
                "7030": "OPPR"
              }
            }
        """

    }

    fun avklaFortsattMedlemskap() {
        //language=JSON
        //     5053   Avklar fortsatt medlemskap.
        """{
              "eksternId": "bc0636ee-b26c-4155-a787-95a82392944c",
              "fagsystem": "K9SAK",
              "saksnummer": "5YC4K",
              "aktørId": "9929038413668",
              "behandlingId": 1000002,
              "eventTid": "2020-03-16T10:38:11.134276",
              "eventHendelse": "BEHANDLINGSKONTROLL_EVENT",
              "behandlinStatus": "UTRED",
              "behandlingStatus": null,
              "behandlingSteg": "VURDERMV",
              "behandlendeEnhet": "4833",
              "ytelseTypeKode": "PSB",
              "behandlingTypeKode": "BT-002",
              "opprettetBehandling": "2020-03-16T10:38",
              "aksjonspunktKoderMedStatusListe": {
                "5053": "OPPR",
                "9001": "OPPR",
                "7030": "UTFO"
              }
            }
        """

    }

    fun startBehandling4() {
        //language=JSON
        """{
              "eksternId": "bc0636ee-b26c-4155-a787-95a82392944c",
              "fagsystem": "K9SAK",
              "saksnummer": "5YC4K",
              "aktørId": "9929038413668",
              "behandlingId": 1000002,
              "eventTid": "2020-03-16T10:38:13.843146",
              "eventHendelse": "BEHANDLINGSKONTROLL_EVENT",
              "behandlinStatus": "UTRED",
              "behandlingStatus": null,
              "behandlingSteg": "VURDER_MEDISINSK",
              "behandlendeEnhet": "4833",
              "ytelseTypeKode": "PSB",
              "behandlingTypeKode": "BT-002",
              "opprettetBehandling": "2020-03-16T10:38",
              "aksjonspunktKoderMedStatusListe": {
                "5053": "UTFO",
                "9001": "OPPR",
                "7030": "UTFO"
              }
            }
        """

    }
}

