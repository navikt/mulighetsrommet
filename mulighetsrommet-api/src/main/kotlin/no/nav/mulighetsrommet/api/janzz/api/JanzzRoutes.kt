package no.nav.mulighetsrommet.api.janzz.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import no.nav.mulighetsrommet.api.clients.pamOntologi.PamOntologiClient
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject

fun Route.janzzRoutes() {
    val pam: PamOntologiClient by inject()

    get("janzz/sertifiseringer/sok", {
        description = "Søk etter sertifiseringer fra Janzz"
        tags = setOf("Janzz")
        operationId = "sokSertifiseringer"
        request {
            queryParameter<String>("q") {
                description = "Søketekst for sertifisering"
                required = true
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = "Liste over sertifiseringer som matcher søket"
                body<List<AmoKategorisering.BransjeOgYrkesrettet.Sertifisering>>()
            }
            default {
                description = "Problem details"
                body<ProblemDetail>()
            }
        }
    }) {
        val q: String by call.request.queryParameters

        val sertifiseringer = sokSertifiseringer(pam, q)
            .asSequence()
            .flatMap { typeaheads ->
                typeaheads.map {
                    AmoKategorisering.BransjeOgYrkesrettet.Sertifisering(
                        konseptId = it.konseptId,
                        label = it.label,
                    )
                }
            }
            // Det finnes noen konsepter med lik label som vi vil filtrere vekk. Det er ikke så
            // viktig hvilken som blir igjen, men sorterer først sånn at det alltid er den samme
            // som blir igjen.
            .sortedBy { it.konseptId }
            .sortedBy { it.label }
            .distinctBy { it.konseptId }
            .distinctBy { it.label }
            .toList()

        call.respond(sertifiseringer)
    }
}

private suspend fun sokSertifiseringer(pam: PamOntologiClient, sok: String) = coroutineScope {
    val autoriseringer = async { pam.sokAutorisasjon(sok) }
    val andreGodkjenninger = async { pam.sokAndreGodkjenninger(sok) }
    awaitAll(autoriseringer, andreGodkjenninger)
}
