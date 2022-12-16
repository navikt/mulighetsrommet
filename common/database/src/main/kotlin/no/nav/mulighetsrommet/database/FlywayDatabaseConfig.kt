package no.nav.mulighetsrommet.database

class FlywayDatabaseConfig(
    host: String,
    port: Int,
    name: String,
    schema: String?,
    user: String,
    password: Password,
    maximumPoolSize: Int,
    googleCloudSqlInstance: String? = null,
    val migrationConfig: FlywayDatabaseAdapter.MigrationConfig = FlywayDatabaseAdapter.MigrationConfig()
) : DatabaseConfig(
    host,
    port,
    name,
    schema,
    user,
    password,
    maximumPoolSize,
    googleCloudSqlInstance
)
