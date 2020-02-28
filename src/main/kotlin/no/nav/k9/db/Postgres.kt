package no.nav.k9.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway
import java.sql.Connection
import javax.sql.DataSource

@KtorExperimentalAPI
fun ApplicationConfig.isVaultEnabled() =
    propertyOrNull("database.vault.mountpath") != null

enum class Role {
     k9los;

    override fun toString() = name.toLowerCase()
}

@KtorExperimentalAPI
fun Application.getDataSource(hikariConfig: HikariConfig) =
    if (environment.config.isVaultEnabled()) {
        dataSourceFromVault(hikariConfig, Role.k9los)
    } else {
        HikariDataSource(hikariConfig)
    }

@KtorExperimentalAPI
fun Application.dataSourceFromVault(hikariConfig: HikariConfig, role: Role) =
    HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
        hikariConfig,
        environment.config.property("database.vault.mountpath").getString(),
        "${environment.config.property("database.name").getString()}-$role"
    )

@KtorExperimentalAPI
fun Application.migrate(hikariConfig: HikariConfig) =
    if (environment.config.isVaultEnabled()) {
        runMigration(dataSourceFromVault(hikariConfig, Role.k9los), "SET ROLE \"${environment.config.property("database.name").getString()}-${Role.k9los}\"")
    } else {
        runMigration(HikariDataSource(hikariConfig))
    }

fun runMigration(dataSource: DataSource, initSql: String? = null): Int {
    return Flyway.configure()
        .locations("migreringer/")
        .dataSource(dataSource)
        .initSql(initSql)
        .load()
        .migrate()
}