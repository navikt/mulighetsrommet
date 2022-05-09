package no.nav.mulighetsrommet.arena_ords_proxy.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.arena_ords_proxy.ArbeidsgiverInfo
import no.nav.mulighetsrommet.arena_ords_proxy.ArenaOrdsClient
import no.nav.mulighetsrommet.arena_ords_proxy.ArenaPersonIdList

fun Route.arenaOrdsRoutes(arenaOrdsClient: ArenaOrdsClient) {
    post("/api/person") {
        runCatching {
            val arenaPersonIdList = call.receive<ArenaPersonIdList>()
            arenaOrdsClient.getFnrByArenaPersonId(arenaPersonIdList)
        }.onSuccess {
            call.respond(it)
        }.onFailure {
            call.respondText("Fail: ${it.stackTraceToString()}", status = HttpStatusCode.InternalServerError)
        }
    }
    get("/api/arbeidsgiver/{arenaArbeidsgiverId}") {
        runCatching {
            val arenaArbeisgiverId = call.parameters["arenaArbeidsgiverId"]!!.toInt()
            arenaOrdsClient.getArbeidsgiverInfoByArenaArbeidsgiverId(arenaArbeisgiverId)
        }.onSuccess {
            call.respond(it)
        }.onFailure {
            call.respondText("Fail: ${it.stackTraceToString()}", status = HttpStatusCode.InternalServerError)
        }
    }
}
