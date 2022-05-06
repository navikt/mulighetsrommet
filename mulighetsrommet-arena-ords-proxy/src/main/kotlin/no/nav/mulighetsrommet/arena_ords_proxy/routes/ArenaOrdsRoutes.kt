package no.nav.mulighetsrommet.arena_ords_proxy.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.mulighetsrommet.arena_ords_proxy.ArenaOrdsClient
import no.nav.mulighetsrommet.arena_ords_proxy.ArenaPersonIdList

fun Route.arenaOrdsRoutes(arenaOrdsClient: ArenaOrdsClient) {
    post("/api/person") {
        val arenaPersonIdList = call.receive<ArenaPersonIdList>()
        val populatedArenaPersonIdList = arenaOrdsClient.getFnrByArenaPersonId(arenaPersonIdList)
        call.respond(populatedArenaPersonIdList)
    }
    get("/api/arbeidsgiver/{arenaArbeidsgiverId}") {
        val arenaArbeisgiverId = call.parameters["arenaArbeisgiverId"]!!.toInt()
        val arbeidsgiverInfo = arenaOrdsClient.getArbeidsgiverInfoByArenaArbeidsgiverId(arenaArbeisgiverId)
        call.respond(arbeidsgiverInfo)
    }
}
