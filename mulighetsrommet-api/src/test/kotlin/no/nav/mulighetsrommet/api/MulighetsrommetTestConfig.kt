package no.nav.mulighetsrommet.api

import com.sksamuel.hoplite.Masked
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.withTestApplication

fun <R> withMulighetsrommetApp(
    config: AppConfig = createTestApplicationConfig(),
    test: TestApplicationEngine.() -> R
): R {
    return withTestApplication({
        configure(config)
    }) {
        test()
    }
}

fun createTestApplicationConfig() = AppConfig(
    database = createDatabaseConfig()
)

fun createDatabaseConfig(
    host: String = "localhost",
    port: Int = 5442,
    name: String = "mulighetsrommet-api-db",
    user: String = "valp",
    password: Masked = Masked("valp")
) = DatabaseConfig(host, port, name, null, user, password)

fun createDatabaseConfigWithRandomSchema(
    host: String = "localhost",
    port: Int = 5442,
    name: String = "mulighetsrommet-api-db",
    user: String = "valp",
    password: Masked = Masked("valp")
): DatabaseConfig {
    val schema = "$name-${java.util.UUID.randomUUID()}"
    return DatabaseConfig(host, port, name, schema, user, password)
}
