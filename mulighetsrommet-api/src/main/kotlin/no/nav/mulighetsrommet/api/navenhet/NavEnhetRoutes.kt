package no.nav.mulighetsrommet.api.navenhet

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject

fun Route.navEnhetRoutes() {
    val navEnhetService: NavEnhetService by inject()

    route("nav-enheter") {
        get({
            tags = setOf("NavEnheter")
            operationId = "getEnheter"
            request {
                queryParameter<List<NavEnhetStatus>>("statuser") {
                    description = "Filtrer på status"
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Alle Nav-enheter"
                    body<List<NavEnhetDbo>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val defaultFilter = EnhetFilter(
                statuser = listOf(
                    NavEnhetStatus.AKTIV,
                    NavEnhetStatus.UNDER_AVVIKLING,
                    NavEnhetStatus.UNDER_ETABLERING,
                ),
            )

            // TODO: ikke returner _alle_ enheter her, de fleste typene er egentlig ikke relevante i adminflate
            val enheter = navEnhetService.hentAlleEnheter(defaultFilter)

            call.respond(enheter)
        }

        get("kostnadssted", {
            tags = setOf("NavEnheter")
            operationId = "getKostnadssted"
            request {
                queryParameter<List<NavEnhetNummer>>("regioner") {
                    description = "Hvilke regioner man henter for"
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Alle kostnadssteder basert på filter"
                    body<List<NavEnhetDbo>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val regioner = call.parameters.getAll("regioner")?.map { NavEnhetNummer(it) } ?: emptyList()
            call.respond(navEnhetService.hentKostnadssted(regioner))
        }

        get("{enhetsnummer}/overordnet", {
            tags = setOf("NavEnheter")
            operationId = "getOverordnetEnhet"
            request {
                pathParameter<NavEnhetNummer>("enhetsnummer") {
                    description = "Enhetsnummer for Nav-enhet"
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Overordnet Nav-enhet for gitt enhetsnummer"
                    body<NavEnhetDbo>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val enhetsnummer = call.parameters.getOrFail("enhetsnummer").let { NavEnhetNummer(it) }

            val overordnetEnhet = navEnhetService.hentOverordnetFylkesenhet(enhetsnummer)
                ?: return@get call.respondText(
                    text = "Fant ikke overordnet enhet for enhetsnummer: $enhetsnummer",
                    status = HttpStatusCode.NotFound,
                )

            call.respond(overordnetEnhet)
        }
    }
}

data class EnhetFilter(
    val statuser: List<NavEnhetStatus>? = null,
    val typer: List<NavEnhetType>? = null,
    val overordnetEnhet: NavEnhetNummer? = null,
)
