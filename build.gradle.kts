import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val dusseldorfKtorVersion = "1.3.2.e71617b"
val ktorVersion = "1.3.2"
val pdfBoxVersion = "2.0.16"
val mainClass = "no.nav.k9.K9LosKt"
val kafkaVersion = "2.3.0" // Alligned med version fra kafka-embedded-env
val hikariVersion = "3.3.1"
val flywayVersion = "6.0.8"
val vaultJdbcVersion = "1.3.1"
val kafkaEmbeddedEnvVersion = "2.2.3"
val cxf = "3.3.1"
val jaxwsTools = "2.3.1"

plugins {
    kotlin("jvm") version "1.3.70"
    id("com.github.johnrengelman.shadow") version "5.1.0"
}

buildscript {
    apply("https://raw.githubusercontent.com/navikt/dusseldorf-ktor/ec226d3ba5b4d5fbc8782d3d934dc5ed0690f85d/gradle/dusseldorf-ktor.gradle.kts")
}

dependencies {
    // Server
    implementation ( "no.nav.helse:dusseldorf-ktor-core:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-ktor-jackson:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-ktor-metrics:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-ktor-health:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-ktor-auth:$dusseldorfKtorVersion")

    // Database
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion")

    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("io.ktor:ktor-html-builder:$ktorVersion")

    // Client
    implementation("no.nav.helse:dusseldorf-ktor-client:$dusseldorfKtorVersion")
    implementation("no.nav.helse:dusseldorf-oauth2-client:$dusseldorfKtorVersion")

    // Kafka
    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")

    implementation("com.sun.istack:istack-commons-runtime:2.2")
    implementation("com.github.seratch:kotliquery:1.3.1")

    // Tilgangskontroll
    implementation("no.nav.common:auth:1.2020.02.18-16.01-aba1e77ea3f9")
    implementation("no.nav.common:rest:1.2020.02.18-16.01-aba1e77ea3f9")
    implementation("com.google.code.gson:gson:2.7")

    // SakOgBehandling
    //<sakogbehandling-tjenestespesifikasjon.version>1.2018.11.06-01.45-c7f37fed0647</sakogbehandling-tjenestespesifikasjon.version>
    implementation("no.nav.tjenestespesifikasjoner:sakogbehandling-tjenestespesifikasjon:1.2020.01.30-14.36-cdf257baea96")
    implementation("no.nav.tjenestespesifikasjoner:nav-virksomhet-hendelsehandterer-behandlingstatus-v1-meldingsdefinisjon:1.2020.01.30-14.36-cdf257baea96")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:${cxf}")
    implementation("org.apache.cxf:cxf-rt-features-logging:${cxf}")
    implementation("org.apache.cxf:cxf-rt-transports-http:${cxf}")
    implementation("org.apache.cxf:cxf-rt-ws-security:${cxf}")
    implementation("org.apache.ws.xmlschema:xmlschema-core:2.2.4") // Force newer version of XMLSchema to fix illegal reflective access warning
    implementation("com.sun.xml.ws:jaxws-tools:${jaxwsTools}") {
        exclude(group = "com.sun.xml.ws", module = "policy")
    }

    implementation("com.ibm.mq:com.ibm.mq.allclient:9.1.0.4")

    // Test
    testImplementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    testImplementation("no.nav:kafka-embedded-env:$kafkaEmbeddedEnvVersion")
    testImplementation("no.nav.helse:dusseldorf-test-support:$dusseldorfKtorVersion")
    testImplementation("io.ktor:ktor-server-test-host:1.3.0") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("org.skyscreamer:jsonassert:1.5.0")

    testImplementation("io.mockk:mockk:1.9.3.kotlin12")
    testImplementation("com.opentable.components:otj-pg-embedded:0.13.3")


    implementation(kotlin("stdlib-jdk8"))
    implementation("javax.ws.rs:javax.ws.rs-api:2.0")

}

repositories {
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://kotlin.bintray.com/kotlinx")
    maven("http://packages.confluent.io/maven/")

    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/navikt/dusseldorf-ktor")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }

    jcenter()
    mavenLocal()
    mavenCentral()
}


java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("app")
    archiveClassifier.set("")
    manifest {
        attributes(
            mapOf(
                "Main-Class" to mainClass
            )
        )
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "6.1.1"
}