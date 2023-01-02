package no.nav.mulighetsrommet.database

interface DatabaseConfig {
    val host: String
    val port: Int
    val name: String
    val schema: String?
    val user: String
    val password: Password
    val maximumPoolSize: Int
    val googleCloudSqlInstance: String?
    val jdbcUrl
        get() = "jdbc:postgresql://$host:$port/$name"
}

@JvmInline
value class Password(val value: String) {
    override fun toString() = "********"
}
