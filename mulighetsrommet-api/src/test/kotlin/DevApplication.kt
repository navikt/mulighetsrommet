import com.sksamuel.hoplite.ConfigLoader
import no.nav.mulighetsrommet.api.Config
import no.nav.mulighetsrommet.api.configure
import no.nav.mulighetsrommet.ktor.startKtorApplication

fun main() {
    val (server, app) = ConfigLoader().loadConfigOrThrow<Config>("/application-local.yaml")

    startKtorApplication(server) {
        configure(app)
    }
}
