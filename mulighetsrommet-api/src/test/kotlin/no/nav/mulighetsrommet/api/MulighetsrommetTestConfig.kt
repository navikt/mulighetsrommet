package no.nav.mulighetsrommet.api

import com.sksamuel.hoplite.Masked
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.withTestApplication

fun <R> withMulighetsrommetApp(config: AppConfig = createTestApplicationConfig(), test: TestApplicationEngine.() -> R): R {
    return withTestApplication({
        configure(config)
    }) {
        test()
    }
}

fun createTestApplicationConfig() = AppConfig(
    database = DatabaseConfig(
        host = "localhost",
        port = 5442,
        name = "mulighetsrommet-api-db",
        user = "valp",
        password = Masked("valp")
    )
)
