package no.nav.mulighetsrommet.api.navenhet

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import no.nav.mulighetsrommet.api.kostnadssted.KostnadsstedService
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.ProblemDetail
import org.koin.ktor.ext.inject

fun Route.navEnhetRoutes() {
    val kostnadsstedService: KostnadsstedService by inject()
    val navEnhetService: NavEnhetService by inject()

    route("nav-enheter") {
        get({
            tags = setOf("NavEnheter")
            operationId = "getEnheter"
            request {
                queryParameter<List<NavEnhetStatus>>("statuser") {
                    description = "Filtrer på status"
                    explode = true
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Alle Nav-enheter"
                    body<List<NavEnhetDto>>()
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

        get("regioner", {
            tags = setOf("NavEnheter")
            operationId = "getRegioner"
            response {
                code(HttpStatusCode.OK) {
                    description = "Alle Nav-enheter"
                    body<List<NavRegionDto>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            call.respond(navEnhetService.hentRegioner())
        }

        get("kostnadssted", {
            tags = setOf("NavEnheter")
            operationId = "getKostnadssted"
            request {
                queryParameter<List<NavEnhetNummer>>("regioner") {
                    description = "Hvilke regioner man henter for"
                    explode = true
                }
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Alle kostnadssteder basert på filter"
                    body<List<NavEnhetDto>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val regioner = call.parameters.getAll("regioner")?.map { NavEnhetNummer(it) } ?: emptyList()
            call.respond(kostnadsstedService.hentKostnadssted(regioner))
        }

        get("kostnadsstedFilter", {
            tags = setOf("NavEnheter")
            operationId = "getKostnadsstedFilter"
            response {
                code(HttpStatusCode.OK) {
                    description = "Filtre for kostnadssteder"
                    body<List<NavRegionDto>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            call.respond(kostnadsstedService.hentKostnadsstedFilter())
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
                    body<NavEnhetDto>()
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
