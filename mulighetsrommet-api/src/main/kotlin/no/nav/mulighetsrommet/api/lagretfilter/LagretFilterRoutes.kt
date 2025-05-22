package no.nav.mulighetsrommet.api.lagretfilter

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.ktor.exception.Forbidden
import no.nav.mulighetsrommet.ktor.plugins.respondWithProblemDetail
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject
import java.util.*

fun Route.lagretFilterRoutes() {
    val lagretFilterService: LagretFilterService by inject()

    route("/lagret-filter") {
        get("mine/{dokumenttype}") {
            val navIdent = getNavIdent()
            val dokumenttype: String by call.parameters

            val filter = lagretFilterService.getLagredeFiltereForBruker(
                brukerId = navIdent.value,
                dokumentType = LagretFilterType.valueOf(dokumenttype),
            )

            call.respond(filter)
        }

        post {
            val navIdent = getNavIdent()
            val request = call.receive<LagretFilter>()

            lagretFilterService.upsertFilter(brukerId = navIdent.value, request)
                .onLeft {
                    call.respondWithProblemDetail(toProblemDetail(it))
                }
                .onRight {
                    call.respond(HttpStatusCode.OK)
                }
        }

        delete("{id}") {
            val navIdent = getNavIdent()
            val id: UUID by call.parameters

            lagretFilterService.deleteFilter(brukerId = navIdent.value, id)
                .onLeft {
                    call.respondWithProblemDetail(toProblemDetail(it))
                }
                .onRight { filterId ->
                    val status = if (filterId == null) HttpStatusCode.NoContent else HttpStatusCode.OK
                    call.respond(status)
                }
        }
    }
}

private fun toProblemDetail(error: LagretFilterError): ProblemDetail = when (error) {
    is LagretFilterError.Forbidden -> Forbidden(error.message)
}
