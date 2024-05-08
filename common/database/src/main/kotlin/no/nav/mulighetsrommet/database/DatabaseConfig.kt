package no.nav.mulighetsrommet.database

data class DatabaseConfig(
    val jdbcUrl: String,
    val schema: String?,
    val maximumPoolSize: Int,
    val googleCloudSqlInstance: String? = null,
) {
    init {
        val postgresPrefix = "jdbc:postgresql://"
        require(jdbcUrl.startsWith(postgresPrefix)) { "jdbcUrl must start with '$postgresPrefix'" }
    }
}

@JvmInline
value class Password(val value: String) {
    override fun toString() = "********"
}
