package no.nav.k9.domene.repository

import no.nav.k9.domene.lager.oppgave.*
import no.nav.k9.domene.modell.AndreKriterierType
import no.nav.k9.domene.modell.BehandlingType

import java.time.LocalDate

class OppgavespørringDto {
    var sortering: KøSortering
    var id: Long
    var behandlingTyper: List<BehandlingType>
    var ytelseTyper: List<FagsakYtelseType>
    var inkluderAndreKriterierTyper: List<AndreKriterierType>
    var ekskluderAndreKriterierTyper: List<AndreKriterierType>
    var isErDynamiskPeriode: Boolean = false
    var filtrerFomDato: LocalDate
    var filtrerTomDato: LocalDate
    var filtrerFra: Long
    var filtrerTil: Long
    var forAvdelingsleder: Boolean = false

    constructor(oppgaveKø: OppgaveKø) {
        sortering = oppgaveKø.sortering
        id = oppgaveKø.avdeling.id
        behandlingTyper = behandlingTypeFra(oppgaveKø)
        ytelseTyper = ytelseType(oppgaveKø)
        inkluderAndreKriterierTyper = inkluderAndreKriterierTyperFra(oppgaveKø)
        ekskluderAndreKriterierTyper = ekskluderAndreKriterierTyperFra(oppgaveKø)
        isErDynamiskPeriode = oppgaveKø.erDynamiskPeriode
        filtrerFomDato = oppgaveKø.fomDato
        filtrerTomDato = oppgaveKø.tomDato
        filtrerFra = oppgaveKø.fra
        filtrerTil = oppgaveKø.til
    }

    constructor(
        id: Long, sortering: KøSortering, behandlingTyper: List<BehandlingType>,
        ytelseTyper: List<FagsakYtelseType>, inkluderAndreKriterierTyper: List<AndreKriterierType>,
        ekskluderAndreKriterierTyper: List<AndreKriterierType>, erDynamiskPeriode: Boolean,
        filtrerFomDato: LocalDate, filtrerTomDato: LocalDate, filtrerFra: Long, filtrerTil: Long
    ) {
        this.sortering = sortering
        this.id = id
        this.behandlingTyper = behandlingTyper
        this.ytelseTyper = ytelseTyper
        this.inkluderAndreKriterierTyper = inkluderAndreKriterierTyper
        this.ekskluderAndreKriterierTyper = ekskluderAndreKriterierTyper
        this.isErDynamiskPeriode = erDynamiskPeriode
        this.filtrerFomDato = filtrerFomDato
        this.filtrerTomDato = filtrerTomDato
        this.filtrerFra = filtrerFra
        this.filtrerTil = filtrerTil
    }

    private fun ekskluderAndreKriterierTyperFra(oppgaveKø: OppgaveKø): List<AndreKriterierType> {
        return emptyList() //oppgaveKø.filtreringAndreKriterierTyper.filter { !it.inkluder }.map { it.andreKriterierType }
    }

    private fun inkluderAndreKriterierTyperFra(oppgaveKø: OppgaveKø): List<AndreKriterierType> {
        return  emptyList() // oppgaveKø.filtreringAndreKriterierTyper.filter { it.inkluder }.map { it.andreKriterierType }
    }

    private fun ytelseType(oppgaveKø: OppgaveKø): List<FagsakYtelseType> {
        return  emptyList() // oppgaveKø.filtreringYtelseTyper.map { it.fagsakYtelseType }
    }

    private fun behandlingTypeFra(oppgaveKø: OppgaveKø): List<BehandlingType> {
        return emptyList() // oppgaveKø.filtreringBehandlingTyper.map { it.behandlingType }
    }
}
