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
    Admin, User, ReadOnly;

    override fun toString() = name.toLowerCase()
}

@KtorExperimentalAPI
fun Application.getDataSource(configuration: Configuration) =
    if (environment.config.isVaultEnabled()) {
        dataSourceFromVault(configuration, Role.User)
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
            dataSourceFromVault(configuration, Role.Admin), "SET ROLE \"${configuration.databaseName()}-${Role.Admin}\""
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