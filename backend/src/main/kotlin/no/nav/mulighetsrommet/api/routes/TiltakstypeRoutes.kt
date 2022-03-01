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
        val tiltakskodeParam = call.parameters["tiltakskode"]!!
        // TODO: To forskjellige feilhåndteringsmetoder. Kan skrives bedre
        runCatching {
            Tiltakskode.valueOf(tiltakskodeParam)
        }.onSuccess { tiltakskode ->
            val tiltakstype =
                tiltakstypeService.getTiltakstypeByTiltakskode(tiltakskode) ?: return@get call.respondText(
                    "Fant ikke tiltakstype med tiltakskode $tiltakskode",
                    status = HttpStatusCode.NotFound
                )
            call.respond(tiltakstype)
        }.onFailure { call.respondText(text = "Ugyldig tiltaskode", status = HttpStatusCode.BadRequest) }
    }
}
