//package no.nav.k9.aksjonspunktbehandling.eventresultat
//
//import no.nav.k9.aksjonspunktbehandling.eventresultat.EventResultat.*
//import no.nav.k9.domene.repository.*
//import no.nav.k9.integrasjon.Aksjonspunkt
//
//class K9SakEventMapper() {
//
//    private fun påVent(åpneAksjonspunkt: List<Aksjonspunkt>): Boolean {
//        return åpneAksjonspunkt.stream()
//            .anyMatch(Aksjonspunkt::erPåVent)
//    }
//
//    private fun manueltSattPåVent(aksjonspunkt: List<Aksjonspunkt>): Boolean {
//        return aksjonspunkt.stream()
//            .anyMatch(Aksjonspunkt::erManueltPåVent)
//    }
//
//    private fun tilBeslutter(aksjonspunkt: List<Aksjonspunkt>): Boolean {
//        return aksjonspunkt.stream()
//            .anyMatch(Aksjonspunkt::tilBeslutter)
//    }
//
//    private fun erSammeEnhet(initiellEnhet: String, nyEnhet: String?): Boolean {
//        return nyEnhet == initiellEnhet
//    }
//
//    private fun erRegistrerPapirsøknad(aksjonspunkt: List<Aksjonspunkt>): Boolean {
//        return aksjonspunkt.stream()
//            .anyMatch(Aksjonspunkt::erRegistrerPapirSøknad)
//    }
//
//    fun signifikantEventFra(
//        eventer: BehandlingProsessEventer
//    ): EventResultat {
//        val åpneAksjonspunkt = eventer.sisteBehandling().åpneAksjonspunkt()
//        val forrige = eventer.nestSisteEvent()
//        val behandlendeEnhet = eventer.sisteEvent().behandlendeEnhet
//        val fraAdmin = false
//
//
//        if (åpneAksjonspunkt.isEmpty()) {
//            return LUKK_OPPGAVE
//        }
//        if (påVent(åpneAksjonspunkt)) {
//            return if (manueltSattPåVent(åpneAksjonspunkt)) LUKK_OPPGAVE_MANUELT_VENT else LUKK_OPPGAVE_VENT
//        }
//
//        val erSammeEnhet = erSammeEnhet(
//            forrige.behandlendeEnhet,
//            behandlendeEnhet
//        )
//
//        if (fraAdmin) {
//            return OPPRETT_OPPGAVE
//        }
//
//        if (tilBeslutter(åpneAksjonspunkt)) {
//            if (forrige.andreKriterierType.tilBeslutter()) {
//                return if (erSammeEnhet
//                ) {
//                    GJENÅPNE_OPPGAVE
//                } else OPPRETT_BESLUTTER_OPPGAVE
//            }
//            return if (forrige.andreKriterierType.tilBeslutter() && erSammeEnhet) {
//                GJENÅPNE_OPPGAVE
//            } else OPPRETT_BESLUTTER_OPPGAVE
//        }
//
//        if (erRegistrerPapirsøknad(åpneAksjonspunkt)) {
//            return if (forrige.andreKriterierType.papirsøknad()) {
//                if (erSammeEnhet
//                ) GJENÅPNE_OPPGAVE else OPPRETT_PAPIRSØKNAD_OPPGAVE
//            } else OPPRETT_PAPIRSØKNAD_OPPGAVE
//        }
//
//        if (forrige.andreKriterierType.papirsøknad()) {
//            return OPPRETT_OPPGAVE
//        }
//
//        if (forrige.andreKriterierType.tilBeslutter()) {
//            return OPPRETT_OPPGAVE
//        }
//
//        return if (erSammeEnhet) GJENÅPNE_OPPGAVE else OPPRETT_OPPGAVE
//    }
//}