//package no.nav.k9.integrasjon
//
//import ResourceLink
//import no.nav.k9.integrasjon.behandling.UtvidetBehandlingDto
//import no.nav.k9.integrasjon.dto.KontrollerFaktaDataDto
//import no.nav.k9.integrasjon.dto.KontrollerFaktaPeriodeDto
//import no.nav.k9.integrasjon.dto.aksjonspunkt.AksjonspunktDto
//import no.nav.k9.integrasjon.dto.inntektarbeidytelse.InntektArbeidYtelseDto
//import no.nav.k9.integrasjon.dto.ytelsefordeling.YtelseFordelingDto
//import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient
//import no.nav.vedtak.sikkerhet.loginmodule.ContainerLogin
//import org.apache.http.client.utils.URIBuilder
//import org.slf4j.Logger
//import org.slf4j.LoggerFactory
//import java.math.BigDecimal
//import java.net.URI
//import java.time.LocalDate
//import java.util.*
//import javax.inject.Inject
//
//
//class K9SakRestKlient {
//
//    private val LOGGER: Logger =
//        LoggerFactory.getLogger(K9SakRestKlient::class.java)
//
//    lateinit var oidcRestClient: OidcRestClient
//
//    private val k9sakBaseUrl = "http://"
//    private val K9SAK_BEHANDLINGER = "/k9/sak/api/behandlinger"
//    private val AKSJONSPUNKTER_LINK = "aksjonspunkter"
//    private val YTELSEFORDELING_LINK = "ytelsefordeling"
//    private val INNTEKT_ARBEID_YTELSE_LINK = "inntekt-arbeid-ytelse"
//    private val UTTAK_KONTROLLER_FAKTA_PERIODER_LINK = "uttak-kontroller-fakta-perioder"
//
//    fun getBehandling(behandlingId: Long): BehandlingK9sak {
//        val uriBuilder =
//            URIBuilder(URI.create(k9sakBaseUrl + K9SAK_BEHANDLINGER))
//        uriBuilder.setParameter("behandlingId", behandlingId.toString())
//        val loginContext = ContainerLogin()
//        loginContext.login()
//
//        return try {
//
//            LOGGER.info("Kaller {}", uriBuilder.build())
//
//            val response: UtvidetBehandlingDto =
//                oidcRestClient.get(uriBuilder.build(), UtvidetBehandlingDto::class.java)
//            val links: List<ResourceLink> = response.getLinks()
//            val aksjonspunkter: List<Aksjonspunkt> = hentAksjonspunkter(links)
//            val behandling = BehandlingK9sak(
//                response.id,
//                response.uuid,
//                response.status.kode,
//                response.behandlingsfristTid,
//                response.type.kode,
//                "",
//                "",
//                response.behandlendeEnhetId,
//                response.behandlendeEnhetNavn,
//                response.ansvarligSaksbehandler,
//                hentFørsteUttaksdato(links),
//                emptyList(),
//                aksjonspunkter,
//                utlandFra(aksjonspunkter),
//                hentHarRefusjonskrav(links),
//                false,
//                false
//            )
//            hentUttakKontrollerFaktaPerioder(behandlingId, behandling, links)
//            return behandling
//        } catch (e: Exception) {
//            throw RuntimeException(e)
//        } finally {
//            loginContext.logout()
//        }
//    }
//
//    private fun velgLink(links: List<ResourceLink>, typeLink: String): Optional<ResourceLink>? {
//        return Optional.ofNullable(links)
//            .orElseGet { emptyList() }
//            .stream()
//            .filter { l: ResourceLink -> l.rel == typeLink }
//            .findFirst()
//    }
//
//    private fun hentUttakKontrollerFaktaPerioder(
//        behandlingId: Long,
//        behandling: BehandlingK9sak,
//        links: List<ResourceLink>
//    ) {
//        val uttakLink =
//            velgLink(
//                links,
//                UTTAK_KONTROLLER_FAKTA_PERIODER_LINK
//            )
//        if (uttakLink != null) {
//            val kontrollerFaktaDataDto: Optional<KontrollerFaktaDataDto> = hentFraResourceLink(
//                uttakLink.get(),
//                KontrollerFaktaDataDto::class.java
//            )
//            if (kontrollerFaktaDataDto.isPresent) {
//                behandling.harGradering =
//                    harGraderingFra(
//                        kontrollerFaktaDataDto.get()
//                    )
//                behandling.harVurderSykdom = harVurderSykdom(kontrollerFaktaDataDto.get())
//            } else {
//                LOGGER.error("Henting av UttakKontrollerFaktaPerioder feilet for behandlingId $behandlingId")
//            }
//        }
//    }
//
//    private fun hentHarRefusjonskrav(links: List<ResourceLink>): Boolean {
//        return velgLink(
//            links,
//            INNTEKT_ARBEID_YTELSE_LINK
//        )!!.flatMap {
//            hentFraResourceLink(
//                it,
//                InntektArbeidYtelseDto::class.java
//            )
//        }.map { harRefusjonskrav(it) }.orElse(null) // har ikke inntektsmelding enda, kan ikke vurdere refusjonskrav
//    }
//
//    private fun utlandFra(aksjonspunkter: List<Aksjonspunkt>): Boolean {
//        return aksjonspunkter.stream()
//            .anyMatch(Aksjonspunkt::erUtenlandssak)
//    }
//
//    private fun hentAksjonspunkter(links: List<ResourceLink>): List<Aksjonspunkt> {
//        return velgLink(
//            links,
//            AKSJONSPUNKTER_LINK
//        )!!.flatMap {
//            hentFraResourceLink(
//                it,
//                Array<AksjonspunktDto>::class.java
//            )
//        }.map {
//            aksjonspunktFraDto(
//                it
//            )
//        }
//            .orElse(emptyList())
//    }
//
//    private fun aksjonspunktFraDto(aksjonspunktDtos: Array<AksjonspunktDto>): List<Aksjonspunkt> {
//        return aksjonspunktDtos.map {
//            Aksjonspunkt(
//                it.definisjon.kode,
//                it.status.kode,
//                it.begrunnelse,
//                it.fristTid
//            )
//        }
//    }
//
//    private fun <T> hentFraResourceLink(
//        resourceLink: ResourceLink,
//        clazz: Class<T>
//    ): Optional<T> {
//        val uri = URI.create(k9sakBaseUrl + resourceLink.href)
//        return if ("POST" == resourceLink.type.name) oidcRestClient.postReturnsOptional(
//            uri,
//            resourceLink.requestPayload,
//            clazz
//        ) else oidcRestClient.getReturnsOptional(uri, clazz)
//    }
//
//    private fun hentFørsteUttaksdato(links: List<ResourceLink>): LocalDate {
//        return velgLink(
//            links,
//            YTELSEFORDELING_LINK
//        )!!
//            .flatMap {
//                hentFraResourceLink(
//                    it,
//                    YtelseFordelingDto::class.java
//                )
//            }.map {
//                it.førsteUttaksdato
//            }
//            .orElse(null)
//    }
//
//    fun harGraderingFra(faktaDataDto: KontrollerFaktaDataDto): Boolean {
//        return faktaDataDto.perioder.map { it.arbeidstidsprosent }.any { it.compareTo(BigDecimal.ZERO) != 0 }
//    }
//
//    private fun harVurderSykdom(kontrollerFaktaDataDto: KontrollerFaktaDataDto): Boolean {
//        return kontrollerFaktaDataDto.perioder.stream()
//            .anyMatch(KontrollerFaktaPeriodeDto::gjelderSykdom)
//    }
//
//    private fun harRefusjonskrav(inntektArbeidYtelseDto: InntektArbeidYtelseDto): Boolean {
//        return inntektArbeidYtelseDto.inntektsmeldinger.map { it.refusjonBeløpPerMnd }.any { it.compareTo(it.ZERO) > 0 }
//    }
//
//}