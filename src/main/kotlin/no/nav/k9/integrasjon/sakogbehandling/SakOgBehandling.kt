package no.nav.k9.integrasjon.sakogbehandling

import no.nav.k9.aksjonspunktbehandling.objectMapper
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingAvsluttet
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingOpprettet
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.ObjectFactory
import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller


fun sendBehandlingOpprettet(behandlingOpprettet: BehandlingOpprettet) {
    val marshaller = JAXBContext.newInstance(BehandlingOpprettet::class.java).createMarshaller()
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
    val writer = StringWriter()
    marshaller.marshal(ObjectFactory().createBehandlingOpprettet(behandlingOpprettet), writer)
    val xml = writer.toString()
    println("Sender behandling opprettet: " + xml)

    println(objectMapper().writeValueAsString(behandlingOpprettet))   // sendTilKø(xml, config)

}

fun sendBehandlingAvsluttet(behandlingAvsluttet: BehandlingAvsluttet) {
    val marshaller = JAXBContext.newInstance(BehandlingAvsluttet::class.java).createMarshaller()
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
    val writer = StringWriter()
    marshaller.marshal(ObjectFactory().createBehandlingAvsluttet(behandlingAvsluttet), writer)
    val xml = writer.toString()
    println("Sender behandling avsluttet: " + xml)
    println(objectMapper().writeValueAsString(behandlingAvsluttet))
    //  sendTilKø(xml, config)
}
