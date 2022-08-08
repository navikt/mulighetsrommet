package no.nav.mulighetsrommet.database

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val schema: String?,
    val user: String,
    val password: Password,
    val maximumPoolSize: Int,
)

@JvmInline
value class Password(val value: String) {
    override fun toString() = "********"
}
