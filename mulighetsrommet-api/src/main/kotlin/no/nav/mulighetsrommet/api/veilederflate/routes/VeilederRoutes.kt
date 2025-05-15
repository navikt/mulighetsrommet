package no.nav.mulighetsrommet.api.veilederflate.routes

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
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.koin.ktor.ext.inject

fun Route.veilederRoutes() {
    val db: ApiDatabase by inject()
    val microsoftGraphClient: MsGraphClient by inject()

    get("/veileder/me") {
        val oid = getNavAnsattEntraObjectId()
        val obo = AccessType.OBO(call.getAccessToken())

        val ansatt = microsoftGraphClient.getNavAnsatt(oid, obo)
        val veileder = NavVeilederDto(
            navIdent = ansatt.navIdent,
            fornavn = ansatt.fornavn,
            etternavn = ansatt.etternavn,
            hovedenhet = NavVeilederDto.Hovedenhet(
                enhetsnummer = ansatt.hovedenhetKode,
                navn = ansatt.hovedenhetNavn,
            ),
        )

        call.respond(veileder)
    }

    route("joyride") {
        post("lagre") {
            val request = call.receive<VeilederJoyrideRequest>()

            val dto = VeilederJoyrideDto(
                navIdent = getNavIdent(),
                fullfort = request.fullfort,
                type = request.joyrideType,
            )
            db.session { queries.veilederJoyride.upsert(dto) }

            call.respond(HttpStatusCode.OK)
        }

        get("{type}/har-fullfort") {
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
    val hovedenhet: Hovedenhet,
) {
    @Serializable
    data class Hovedenhet(
        val enhetsnummer: NavEnhetNummer,
        val navn: String,
    )
}
