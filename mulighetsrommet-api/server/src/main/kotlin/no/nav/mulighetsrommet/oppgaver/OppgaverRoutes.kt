package no.nav.mulighetsrommet.oppgaver

import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.oppgaverRoutes() {
    val db: ApiDatabase by inject()
    val service: OppgaverService by inject()

    route("oppgaver") {
        post({
            tags = setOf("Oppgaver")
            operationId = "getOppgaver"
            request {
                body<OppgaverFilter>()
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Oppgaver utledet basert på rollene til innlogget bruker"
                    body<List<Oppgave>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val navIdent = getNavIdent()
            val ansatt = db.session { queries.ansatt.getOrError(navIdent) }

            val filter = call.receive<OppgaverFilter>()

            val oppgaver = service.oppgaver(
                oppgavetyper = filter.oppgavetyper,
                tiltakskoder = filter.tiltakskoder,
                navEnheter = filter.navEnheter,
                arrangorer = filter.arrangorer.toSet(),
                ansatt = ansatt,
            )

            call.respond(oppgaver)
        }

        get("oppgavetyper", {
            tags = setOf("Oppgaver")
            operationId = "getOppgavetyper"
            response {
                code(HttpStatusCode.OK) {
                    description = "Relevante oppgavetyper basert på rollene til innlogget bruker"
                    body<List<OppgaveTypeDto>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val navIdent = getNavIdent()
            val ansatt = db.session { queries.ansatt.getOrError(navIdent) }

            val oppgavetyper = service.getOppgavetyper(ansatt)

            call.respond(oppgavetyper)
        }
    }
}

@Serializable
data class OppgaverFilter(
    val oppgavetyper: Set<OppgaveType>,
    val tiltakskoder: Set<Tiltakskode>,
    val navEnheter: Set<NavEnhetNummer>,
    val arrangorer: Set<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        >,
)

@Serializable
data class OppgaveTypeDto(
    val navn: String,
    val type: OppgaveType,
)
