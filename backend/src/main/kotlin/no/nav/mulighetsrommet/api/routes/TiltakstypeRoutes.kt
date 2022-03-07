package no.nav.mulighetsrommet.api.routes

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.mulighetsrommet.api.domain.Tiltakskode
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import org.koin.ktor.ext.inject

fun Route.tiltakstypeRoutes() {

    val tiltakstypeService: TiltakstypeService by inject()

    get("/api/tiltakstyper") {
        val tiltakstyper = tiltakstypeService.getTiltakstyper()
        call.respond(tiltakstyper)
    }
    get("/api/tiltakstyper/{tiltakskode}") {
        runCatching {
            val tiltakskode = Tiltakskode.valueOf(call.parameters["tiltakskode"]!!)
            tiltakstypeService.getTiltakstypeByTiltakskode(tiltakskode)
        }.onSuccess { tiltakstype ->
            call.respond(tiltakstype!!)
        }.onFailure {
            call.respondText(text = "Fant ikke tiltakstype", status = HttpStatusCode.NotFound)
        }
    }
}
