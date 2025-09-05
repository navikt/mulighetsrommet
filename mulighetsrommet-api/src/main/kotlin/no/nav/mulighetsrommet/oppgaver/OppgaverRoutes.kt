package no.nav.mulighetsrommet.oppgaver

import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.http.content.default
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.plugins.queryParameterUuid
import no.nav.mulighetsrommet.api.tilsagn.api.TilsagnDto
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnRequest
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.model.DataDrivenTableDto
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.Tiltakskode
import org.koin.ktor.ext.inject

fun Route.oppgaverRoutes() {
    val db: ApiDatabase by inject()
    val service: OppgaverService by inject()

    fun RoutingContext.getAnsatt(): NavAnsatt = db.session {
        val navIdent = getNavIdent()

        queries.ansatt.getByNavIdent(navIdent)
            ?: throw IllegalStateException("Fant ikke ansatt med navIdent=$navIdent")
    }

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
            val ansatt = getAnsatt()
            val filter = call.receive<OppgaverFilter>()

            val oppgaver = service.oppgaver(
                oppgavetyper = filter.oppgavetyper,
                tiltakskoder = filter.tiltakskoder,
                regioner = filter.regioner,
                ansatt = ansatt.navIdent,
                roller = ansatt.roller,
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
            val ansatt = getAnsatt()

            val ansattesRoller = ansatt.roller.map { it.rolle }
            val oppgavetyper = OppgaveType.entries.filter { it.rolle in ansattesRoller }.map {
                OppgaveTypeDto(navn = it.navn, type = it)
            }

            call.respond(oppgavetyper)
        }
    }
}

@Serializable
data class OppgaverFilter(
    val oppgavetyper: Set<OppgaveType>,
    val tiltakskoder: Set<Tiltakskode>,
    val regioner: Set<NavEnhetNummer>,
)

@Serializable
data class OppgaveTypeDto(
    val navn: String,
    val type: OppgaveType,
)
