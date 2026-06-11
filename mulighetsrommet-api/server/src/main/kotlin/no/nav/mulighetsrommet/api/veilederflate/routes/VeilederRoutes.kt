package no.nav.mulighetsrommet.api.veilederflate.routes

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.clients.msgraph.MsGraphClient
import no.nav.mulighetsrommet.api.plugins.getAccessType
import no.nav.mulighetsrommet.api.plugins.getNavAnsattEntraObjectId
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject

fun Route.veilederRoutes() {
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
        val obo = call.getAccessType()

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
