package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.clients.AccessType
import no.nav.mulighetsrommet.api.clients.pamOntologi.PamOntologiClient
import no.nav.mulighetsrommet.domain.dto.AmoKategorisering
import no.nav.mulighetsrommet.ktor.extensions.getAccessToken
import org.koin.ktor.ext.inject

fun Route.janzzRoutes() {
    val pam: PamOntologiClient by inject()

    route("/api/v1/intern/janzz") {
        get("sertifiseringer/sok") {
            val q: String by call.request.queryParameters
            val obo = AccessType.OBO(call.getAccessToken())
            call.respond(
                (pam.sokAutorisasjon(q, obo) + pam.sokAndreGodkjenninger(q, obo))
                    .map {
                        AmoKategorisering.Sertifisering(
                            konseptId = it.konseptId,
                            label = it.label,
                        )
                    },
            )
        }
    }
}
