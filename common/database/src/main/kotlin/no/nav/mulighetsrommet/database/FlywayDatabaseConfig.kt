package no.nav.mulighetsrommet.database

data class FlywayDatabaseConfig(
    override val host: String,
    override val port: Int,
    override val name: String,
    override val schema: String?,
    override val user: String,
    override val password: Password,
    override val maximumPoolSize: Int,
    override val googleCloudSqlInstance: String? = null,
    val migrationConfig: FlywayDatabaseAdapter.MigrationConfig = FlywayDatabaseAdapter.MigrationConfig()
) : DatabaseConfig
