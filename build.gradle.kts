import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val dusseldorfKtorVersion = "1.3.2.12404e4"
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
val koinVersion = "2.1.6"

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
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-auth-basic:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    
    // Kafka
    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")

    implementation("com.sun.istack:istack-commons-runtime:2.2")
    implementation("com.github.seratch:kotliquery:1.3.1")

    // Tilgangskontroll
    implementation("no.nav.common:auth:1.2020.02.18-16.01-aba1e77ea3f9")
    implementation("no.nav.common:rest:1.2020.02.18-16.01-aba1e77ea3f9")
    implementation("com.google.code.gson:gson:2.7")
    
    // Kontrakter
    implementation("no.nav.k9.sak:kodeverk:3.1.0-20200902071025-3d40c5c")
    implementation("no.nav.k9.sak:kontrakt:3.1.0-20200902071025-3d40c5c")
    implementation("no.nav.k9.statistikk:kontrakter:2.0_20200514170610_720949d")

    // Div
    implementation("info.debatty:java-string-similarity:1.2.1")
    implementation("com.papertrailapp:logback-syslog4j:1.0.0")

    // DI
    implementation("org.koin:koin-core:$koinVersion")
    implementation("org.koin:koin-ktor:$koinVersion")
    
    // Test
    testImplementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    testImplementation("no.nav:kafka-embedded-env:$kafkaEmbeddedEnvVersion")
    testImplementation("no.nav.helse:dusseldorf-test-support:$dusseldorfKtorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8")
    testImplementation("io.mockk:mockk:1.10.0")
    testImplementation("io.ktor:ktor-server-test-host:1.3.0") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("org.skyscreamer:jsonassert:1.5.0")

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
    isZip64 = true
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