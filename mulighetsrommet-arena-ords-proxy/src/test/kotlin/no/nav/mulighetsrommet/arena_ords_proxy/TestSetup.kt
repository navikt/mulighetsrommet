package no.nav.mulighetsrommet.arena_ords_proxy

import com.sksamuel.hoplite.Masked
import io.ktor.server.testing.*

fun <R> withArenaOrdsProxyApp(
    arenaOrdsClient: ArenaOrdsClient = ArenaOrdsClient(ArenaOrdsConfig("", "", Masked(""))),
    test: suspend ApplicationTestBuilder.() -> R
) {
    testApplication {
        application {
            configure(arenaOrdsClient)
        }
        test()
    }
}
