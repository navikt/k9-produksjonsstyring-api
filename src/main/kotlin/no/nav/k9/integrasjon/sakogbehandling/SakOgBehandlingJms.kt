package no.nav.k9.integrasjon.sakogbehandling

import com.ibm.mq.MQC
import com.ibm.mq.jms.MQConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingAvsluttet
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingOpprettet
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.ObjectFactory
import java.io.StringWriter
import javax.jms.MessageProducer
import javax.jms.Session
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller

fun connectionFactory(
    hostName: String,
    port: Int,
    gatewayName: String,
    channelName: String
) = MQConnectionFactory().apply {
    this.hostName = hostName
    this.port = port
    this.queueManager = gatewayName
    this.transportType = WMQConstants.WMQ_CM_CLIENT
    this.channel = channelName
    this.ccsid = 1208
    setIntProperty(WMQConstants.JMS_IBM_ENCODING, MQC.MQENC_NATIVE)
    setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, 1208)
}

fun Session.producerForQueue(queueName: String): MessageProducer = createProducer(createQueue(queueName))

fun sendBehandlingAvsluttet(behandlingAvsluttet: BehandlingAvsluttet) {
    val marshaller = JAXBContext.newInstance(BehandlingAvsluttet::class.java).createMarshaller()
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
    val writer = StringWriter()
    marshaller.marshal(ObjectFactory().createBehandlingAvsluttet(behandlingAvsluttet), writer)
    val xml = writer.toString()
    sendTilKø(xml)
}

fun sendBehandlingOpprettet(behandlingOpprettet: BehandlingOpprettet) {
    val marshaller = JAXBContext.newInstance(BehandlingOpprettet::class.java).createMarshaller()
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
    val writer = StringWriter()
    marshaller.marshal(ObjectFactory().createBehandlingOpprettet(behandlingOpprettet), writer)
    val xml = writer.toString()
    sendTilKø(xml)
}

private fun sendTilKø(xml: String) {
    val connection = connectionFactory(
        hostName = "e26apvl121.test.local",
        port = 1411,
        gatewayName = "MUXLSC01",
        channelName = "QA.U_SAKOGBEHANDLING.SAKSBEHANDLING"
    ).createConnection("", "")
    val session = connection.createSession()
    val producer = session.createProducer(session.createQueue("DEV.QUEUE.1"))
    connection.start()
    producer.send(session.createTextMessage(xml))
    connection.stop()
}

