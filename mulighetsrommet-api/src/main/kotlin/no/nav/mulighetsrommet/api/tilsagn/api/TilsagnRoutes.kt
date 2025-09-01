package no.nav.mulighetsrommet.api.tilsagn.api

import io.ktor.server.routing.*

fun Route.tilsagnRoutes() {
    route("tilsagn") {
        tilsagnRoutesGetAll()
        tilsagnRoutesGet()
        tilsagnRoutesBehandling()
        tilsagnRoutesBeregning()
    }
}
