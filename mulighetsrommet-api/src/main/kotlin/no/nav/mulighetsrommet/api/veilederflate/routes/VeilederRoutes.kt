package no.nav.mulighetsrommet.api.veilederflate.routes

import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.msgraph.MsGraphClient
import no.nav.mulighetsrommet.api.plugins.getNavAnsattEntraObjectId
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.veilederflate.models.JoyrideType
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederJoyrideDto
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederJoyrideRequest
import no.nav.mulighetsrommet.ktor.extensions.getAccessToken
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.koin.ktor.ext.inject

fun Route.veilederRoutes() {
    val db: ApiDatabase by inject()
    val microsoftGraphClient: MsGraphClient by inject()

    get("/me", {
        description = "Informasjon om innlogget Nav-veileder"
        tags = setOf("Veileder")
        operationId = "getVeileder"
        response {
            code(HttpStatusCode.OK) {
                description = "Informasjon om veileder"
                body<NavVeilederDto>()
            }
            default {
                description = "En feil har oppstått"
                body<ProblemDetail>()
            }
        }
    }) {
        val oid = getNavAnsattEntraObjectId()
        val obo = AccessType.OBO(call.getAccessToken())

        val ansatt = microsoftGraphClient.getNavAnsatt(oid, obo)
        val veileder = NavVeilederDto(
            navIdent = ansatt.navIdent,
            fornavn = ansatt.fornavn,
            etternavn = ansatt.etternavn,
            hovedenhet = NavVeilederHovedenhet(
                enhetsnummer = ansatt.hovedenhetKode,
                navn = ansatt.hovedenhetNavn,
            ),
        )

        call.respond(veileder)
    }

    route("joyride") {
        post("lagre", {
            description = "Lagrer at en veileder er ferdig med en joyride"
            tags = setOf("Joyride")
            operationId = "lagreJoyrideHarKjort"
            request {
                body<VeilederJoyrideRequest> {
                    required = true
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Lagret ok"
                }
                default {
                    description = "En feil har oppstått"
                    body<ProblemDetail>()
                }
            }
        }) {
            val request = call.receive<VeilederJoyrideRequest>()

            val dto = VeilederJoyrideDto(
                navIdent = getNavIdent(),
                fullfort = request.fullfort,
                type = request.joyrideType,
            )
            db.session { queries.veilederJoyride.upsert(dto) }

            call.respond(HttpStatusCode.OK)
        }

        get("{type}/har-fullfort", {
            description = "Finner ut om en veileder har fullført en joyride"
            tags = setOf("Joyride")
            operationId = "veilederHarFullfortJoyride"
            request {
                pathParameter<JoyrideType>("type") {
                    description = "Type joyride som skal sjekkes"
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Indikerer om veileder har fullført joyride"
                    body<Boolean>()
                }
                default {
                    description = "En feil har oppstått"
                    body<ProblemDetail>()
                }
            }
        }) {
            val navIdent = getNavIdent()
            val type = call.parameters.getOrFail("type").let { JoyrideType.valueOf(it) }

            val fullfort = db.session {
                queries.veilederJoyride.harFullfortJoyride(navIdent, type)
            }

            call.respond(fullfort)
        }
    }
}

@Serializable
data class NavVeilederDto(
    val navIdent: NavIdent,
    val fornavn: String,
    val etternavn: String,
    val hovedenhet: NavVeilederHovedenhet,
)

@Serializable
data class NavVeilederHovedenhet(
    val enhetsnummer: NavEnhetNummer,
    val navn: String,
)
