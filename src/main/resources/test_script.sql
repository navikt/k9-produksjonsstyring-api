insert into oppgave
values (1,
        '{
          "behandlingId": 743907,
          "fagsaksNummer": 3298043,
          "behandlendeEnhet": "behEnhet",
          "behandlingsfrist": {
            "localDate": "2019-02-05",
            "localDateTime": "2019-02-05T22:08:28.097832",
            "localTime": "22:08:28.097869"
          },
          "behandlingOpprettet": {
            "localDate": "2018-02-05",
            "localDateTime": "2018-02-05T22:08:28.097832",
            "localTime": "22:08:28.097869"
          },
          "forsteStonadsdag": {
            "localDate": "2018-02-05"
          },
          "behandlingStatus": {
            "kode": "OPPRE"
          },
          "behandlingType": {
            "kode": "BT-004",
            "navn": "Revurdering"
          },
          "fagsakYtelseType": {
            "kode": "FP",
            "navn": "Foreldrepenger"
          },
          "aktiv": true,
          "system": "fdgb@gb",
          "oppgaveAvsluttet": {
            "localDate": "2019-02-20",
            "localDateTime": "2019-02-20T22:08:28.097832",
            "localTime": "22:08:28.097869"
          },
          "utfortFraAdmin": false,
          "eksternId": "6ba7b811-9dad-11d1-80b4-00c04fd430c8",
          "reservasjon": null,
          "oppgaveEgenskap": [
            {
              "id": 938475545,
              "andreKriterierType": {
                "kode": "VURDER_SYKDOM",
                "navn": "Vurder sykdom"
              },
              "sisteSaksbehandlerForTotrinn": "lrketyihgtklroi",
              "aktiv": true
            }
          ]
        }
        ',
        'VL',
        default,
        null,
        null,
        default)

as

select data from oppgave where data -> 'behandlingType' = ANY('{"kljhgrerghtre}') and  data -> 'fagsakYtelseType' = ANY('{"kjlhkl"}')
                           and 'fgljr' = ANY(data -> 'oppgaveEgenskap' ->> 'andreKriterierTyper')
                           and not 'dslkfj' = ANY(data -> 'oppgaveEgenskap' ->> 'andreKriterierTyper')
                           and data -> 'oppgaveEgenskap' ->> 'aktiv' = 'false' and  data -> 'oppgaveEgenskap' ->> 'andreKriterierTyper' ->> 'kode' != 'TIL_BESLUTTER'
                           and
                data -> 'oppgaveEgenskap' ->> 'sisteSaksbehandlerForTotrinn' != 'gefdr'


