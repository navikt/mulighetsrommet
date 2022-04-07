package no.nav.mulighetsrommet.api.routes

import io.ktor.application.call
import io.ktor.features.BadRequestException
import io.ktor.http.*
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import no.nav.mulighetsrommet.api.services.TiltaksgjennomforingService
import no.nav.mulighetsrommet.api.services.TiltakstypeService
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.Tiltakstype
import org.koin.ktor.ext.inject

// TODO: Må lage noe felles validering her etterhvert
fun Parameters.parseList(parameter: String): List<String> {
    return entries().filter { it.key == parameter }.flatMap { it.value }
}

fun Route.tiltakstypeRoutes() {

    val tiltakstypeService: TiltakstypeService by inject()
    val tiltaksgjennomforingService: TiltaksgjennomforingService by inject()

    get("/api/tiltakstyper") {
        val search = call.request.queryParameters["search"]

        val innsatsgrupper = call.request.queryParameters["innsatsgrupper"]?.toInt()

        val items = tiltakstypeService.getTiltakstyper(innsatsgrupper, search)
        call.respond(items)
    }
    get("/api/tiltakstyper/{tiltakskode}") {
        call.application.environment.log.debug("her??")
        runCatching {
            val tiltakskode = Tiltakskode.valueOf(call.parameters["tiltakskode"]!!)
            tiltakstypeService.getTiltakstypeByTiltakskode(tiltakskode)
        }.onSuccess { fetchedTiltakstype ->
            if (fetchedTiltakstype != null) {
                call.respond(fetchedTiltakstype)
            }
            call.respondText(text = "Fant ikke tiltakstype", status = HttpStatusCode.NotFound)
        }.onFailure {
            call.application.environment.log.error(it.stackTraceToString())
            call.respondText(text = "Fant ikke tiltakstype", status = HttpStatusCode.NotFound)
        }
    }
    get("/api/tiltakstyper/{tiltakskode}/tiltaksgjennomforinger") {
        runCatching {
            val tiltakskode = Tiltakskode.valueOf(call.parameters["tiltakskode"]!!)
            tiltaksgjennomforingService.getTiltaksgjennomforingerByTiltakskode(tiltakskode)
        }.onSuccess { fetchedTiltaksgjennomforinger ->
            call.respond(fetchedTiltaksgjennomforinger)
        }.onFailure { call.respondText("Fant ikke tiltakstype", status = HttpStatusCode.NotFound) }
    }
    post("/api/tiltakstyper") {
        runCatching {
            val tiltakstype = call.receive<Tiltakstype>()
            tiltakstypeService.createTiltakstype(tiltakstype)
        }.onSuccess { createdTiltakstype ->
            call.response.status(HttpStatusCode.Created)
            call.respond(createdTiltakstype)
        }.onFailure {
            call.respondText("Kunne ikke opprette tiltakstype", status = HttpStatusCode.InternalServerError)
        }
    }
    put("/api/tiltakstyper/{tiltakskode}") {
        call.application.environment.log.debug("her??")
        runCatching {
            call.application.environment.log.debug("hallo?????")
            println("faen som skjer")
            val tiltakskode = Tiltakskode.valueOf(call.parameters["tiltakskode"]!!)
            val tiltakstype = call.receive<Tiltakstype>()
            tiltakstypeService.updateTiltakstype(tiltakskode, tiltakstype)
        }.onSuccess { updatedTiltakstype ->
            call.respond(updatedTiltakstype)
        }
    }
}
