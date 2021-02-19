import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val dusseldorfKtorVersion = "1.4.0.8634f4b"
val ktorVersion = "1.4.0"
val mainClass = "no.nav.k9.K9LosKt"
val kafkaVersion = "2.3.0" // Alligned med version fra kafka-embedded-env
val hikariVersion = "3.4.5"
val flywayVersion = "6.0.8"
val vaultJdbcVersion = "1.3.7"
val kafkaEmbeddedEnvVersion = "2.2.3"
val koinVersion = "2.2.2"
val kotliqueryVersion = "1.3.1"

plugins {
    kotlin("jvm") version "1.4.30"
    id("com.github.johnrengelman.shadow") version "6.1.0"
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
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")

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


    // Tilgangskontroll
    implementation("no.nav.common:auth:2.2021.02.08_08.29-beea07de78ad")
    implementation("no.nav.common:rest:2.2021.02.08_08.29-beea07de78ad")
    implementation("com.google.code.gson:gson:2.8.6")

    // Kontrakter
    implementation("no.nav.k9.sak:kontrakt:3.1.15")
    implementation("no.nav.k9.statistikk:kontrakter:2.0_20201201123022_bfccad8")

    // Div
    implementation("info.debatty:java-string-similarity:2.0.0")
    implementation("com.papertrailapp:logback-syslog4j:1.0.0")

    // DI
    implementation("org.koin:koin-core:$koinVersion")
    implementation("org.koin:koin-ktor:$koinVersion")

    // Test
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.23")
    testImplementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    testImplementation("no.nav:kafka-embedded-env:$kafkaEmbeddedEnvVersion")
    testImplementation("no.nav.helse:dusseldorf-test-support:$dusseldorfKtorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8")
    testImplementation("io.mockk:mockk:1.10.5")
    testImplementation("io.ktor:ktor-server-test-host:1.3.0") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("org.skyscreamer:jsonassert:1.5.0")

    testImplementation("com.opentable.components:otj-pg-embedded:0.13.3")
    testImplementation("org.koin:koin-test:$koinVersion")

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
