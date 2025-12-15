package no.nav.mulighetsrommet.api.tilsagn.api

import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.tilsagnRoutes() {
    route("tilsagn") {
        tilsagnRoutesGetAll()
        tilsagnRoutesGet()
        tilsagnRoutesBehandling()
        tilsagnRoutesBeregning()
    }
}
