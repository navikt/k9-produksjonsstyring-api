package no.nav.k9.tilgangskontroll.naudit

import no.nav.k9.tilgangskontroll.naudit.Audit.AuditResource

class AuditResources {
    class Enhet {
        companion object {
            val Kontaktinformasjon = AuditResource("enhet.kontaktinformasjon")
            val Ansatte = AuditResource("enhet.ansatte")
            val OppgaveBehandlere = AuditResource("enhet.oppgavebehandlere")
            val Foreslatte = AuditResource("enhet.foreslatte")
        }
    }

    class Saksbehandler {
        companion object {
            @JvmField
            val NavnOgEnheter = AuditResource("saksbehandler.navnOgEnheter")
            @JvmField
            val Enheter = AuditResource("saksbehandler.enheter")
            @JvmField
            val ValgtEnhet = AuditResource("saksbehandler.valgtenhet")
            val EgenAnsatt = AuditResource("saksbehandler.egenansatt")
            val Roller = AuditResource("saksbehandler.roller")
        }
    }

    class Person {
        class Brukerprofil {
            companion object {
                val Navn = AuditResource("brukerprofil.navn")
                val Adresse = AuditResource("brukerprofil.adresse")
                val TilrettelagtKommunikasjon = AuditResource("brukerprofil.tilrettelagtkommunikasjon")
                val Telefonnummer = AuditResource("brukerprofil.telefonnummer")
                val Kontonummer = AuditResource("brukerprofil.kontonummer")
            }
        }

        class Henvendelse {
            companion object {
                @JvmField
                val Les = AuditResource("person.henvendelse.les")
                @JvmField
                val Sok = AuditResource("person.henvendelse.sok")
                val Opprettet = AuditResource("person.henvendelse.opprettet")
                val Ferdigstill = AuditResource("person.henvendelse.ferdigstill")
                val SlaSammen = AuditResource("person.henvendelse.slasammen")
                val Print = AuditResource("person.henvendelse.print")
                val Delsvar = AuditResource("person.henvendelse.delsvar")
                @JvmField
                val Journalfor = AuditResource("person.henvendelse.journalfor")
            }

            class Merk {
                companion object {
                    val Feilsendt = AuditResource("person.henvendelse.merk.feilsendt")
                    val Bidrag = AuditResource("person.henvendelse.merk.feilsendt")
                    val Kontorsperre = AuditResource("person.henvendelse.merk.feilsendt")
                    val Avslutt = AuditResource("person.henvendelse.merk.feilsendt")
                    val Slett = AuditResource("person.henvendelse.merk.feilsendt")
                }
            }

            class Oppgave {
                companion object {
                    val Opprett = AuditResource("person.henvendelse.oppgave.opprett")
                    val LeggTilbake = AuditResource("person.henvendelse.oppgave.leggTilbake")
                    val Plukk = AuditResource("person.henvendelse.oppgave.plukk")
                    val Tildelte = AuditResource("person.henvendelse.oppgave.tildel")
                    val Avslutt = AuditResource("person.henvendelse.oppgave.avslutt")
                }
            }
        }

        companion object {
            val Kontaktinformasjon = AuditResource("person.kontaktinformasjon")
            val Oppfolging = AuditResource("person.oppfolging")
            @JvmField
            val Ytelser = AuditResource("person.ytelser")
            @JvmField
            val Kontrakter = AuditResource("person.kontrakter")
            val YtelserOgKontrakter = AuditResource("person.ytelserogkontrakter")
            @JvmField
            val Personalia = AuditResource("person.personalia")
            val Saker = AuditResource("person.saker")
            @JvmField
            val GsakSaker = AuditResource("person.gsaksaker")
            @JvmField
            val PesysSaker = AuditResource("person.pesyssaker")
            val Dokumenter = AuditResource("person.dokumenter")
            val Utbetalinger = AuditResource("person.utbetalinger")
            val Vergemal = AuditResource("person.vergemal")
            @JvmField
            val Sykepenger = AuditResource("person.sykepenger")
            @JvmField
            val Foreldrepenger = AuditResource("person.foreldrepenger")
            @JvmField
            val Pleiepenger = AuditResource("person.pleiepenger")
            @JvmField
            val Varsler = AuditResource("person.varsler")
        }
    }

    class Personsok {
        companion object {
            val Resultat = AuditResource("personsok.resultat")
        }
    }
}