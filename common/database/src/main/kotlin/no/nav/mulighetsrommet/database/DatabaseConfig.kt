package no.nav.mulighetsrommet.database

import com.zaxxer.hikari.HikariConfig

private const val JDBC_POSTGRESQL_PREFIX = "jdbc:postgresql://"

data class DatabaseConfig(
    val jdbcUrl: String,
    val schema: String? = null,
    val maximumPoolSize: Int,
    val googleCloudSqlInstance: String? = null,
    val additionalConfig: HikariConfig.() -> Unit = {},
) {
    init {
        require(jdbcUrl.startsWith(JDBC_POSTGRESQL_PREFIX)) { "jdbcUrl must start with '$JDBC_POSTGRESQL_PREFIX'" }
    }

    fun getDatabaseName(): String {
        val databaseNameRegex = Regex("$JDBC_POSTGRESQL_PREFIX[^/]+/([\\w\\-]+)")
        return requireNotNull(databaseNameRegex.find(jdbcUrl)?.groupValues?.get(1))
    }
}

@JvmInline
value class Password(val value: String) {
    override fun toString() = "********"
}
