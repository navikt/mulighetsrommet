package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import no.nav.mulighetsrommet.api.clients.pamOntologi.PamOntologiClient
import no.nav.mulighetsrommet.domain.dto.AmoKategorisering
import org.koin.ktor.ext.inject

fun Route.janzzRoutes() {
    val pam: PamOntologiClient by inject()

    route("/api/v1/intern/janzz") {
        get("sertifiseringer/sok") {
            val q: String by call.request.queryParameters

            val autoriseringer = async { pam.sokAutorisasjon(q) }
            val andreGodkjenninger = async { pam.sokAndreGodkjenninger(q) }

            val sertifiseringer = awaitAll(autoriseringer, andreGodkjenninger)
                .flatMap { typeaheads ->
                    typeaheads.map {
                        AmoKategorisering.Sertifisering(
                            konseptId = it.konseptId,
                            label = it.label,
                        )
                    }
                }
                .distinctBy { it.konseptId }

            call.respond(sertifiseringer)
        }
    }
}
