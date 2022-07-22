package no.nav.mulighetsrommet.arena.adapter

import com.sksamuel.hoplite.Masked

fun createDatabaseConfigWithRandomSchema(
    host: String = "localhost",
    port: Int = 5443,
    name: String = "mulighetsrommet-arena-adapter-db",
    user: String = "valp",
    password: Masked = Masked("valp")
): DatabaseConfig {
    val schema = "${java.util.UUID.randomUUID()}"
    return DatabaseConfig(host, port, name, schema, user, password)
}
