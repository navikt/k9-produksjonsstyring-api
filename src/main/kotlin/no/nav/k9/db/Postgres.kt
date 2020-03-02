package no.nav.k9.db

import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.k9.Configuration
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway
import javax.sql.DataSource

@KtorExperimentalAPI
fun ApplicationConfig.isVaultEnabled() =

    propertyOrNull("database.vault.mountpath") != null

enum class Role {
     k9los;

    override fun toString() = name.toLowerCase()
}

@KtorExperimentalAPI
fun Application.getDataSource(configuration: Configuration) =
    if (environment.config.isVaultEnabled()) {
        dataSourceFromVault(configuration, Role.k9los)
    } else {
        HikariDataSource(configuration.hikariConfig())
    }

@KtorExperimentalAPI
fun Application.dataSourceFromVault(hikariConfig: Configuration, role: Role) =
    HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
        hikariConfig.hikariConfig(),
        hikariConfig.getVaultDbPath(),
        "${hikariConfig.databaseName()}-$role"
    )

@KtorExperimentalAPI
fun Application.migrate(configuration: Configuration) =
    if (configuration.isVaultEnabled()) {
        runMigration(
            dataSourceFromVault(configuration, Role.k9los), "SET ROLE \"${configuration.databaseName()}-${Role.k9los}\""
        )
    } else {
        runMigration(HikariDataSource(configuration.hikariConfig()))
    }

fun runMigration(dataSource: DataSource, initSql: String? = null): Int {
    return Flyway.configure()
        .locations("migreringer/")
        .dataSource(dataSource)
        .initSql(initSql)
        .load()
        .migrate()
}