package no.nav.mulighetsrommet.database

import com.sksamuel.hoplite.Masked

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val schema: String?,
    val user: String,
    val password: Masked,
    val maximumPoolSize: Int,
)
