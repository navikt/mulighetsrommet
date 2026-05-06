package no.nav.mulighetsrommet.api.avtale.api

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.http.content.default
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import no.nav.mulighetsrommet.api.amo.OpplaringKategoriseringMapper
import no.nav.mulighetsrommet.api.amo.OpplaringKategoriseringResponse
import no.nav.mulighetsrommet.api.avtale.model.AvtaletypeInfo
import no.nav.mulighetsrommet.api.avtale.model.PrismodellInfo
import no.nav.mulighetsrommet.api.avtale.model.Prismodeller
import no.nav.mulighetsrommet.api.janzz.PamOntologiService
import no.nav.mulighetsrommet.api.janzz.Sertifisering
import no.nav.mulighetsrommet.ktor.exception.BadRequest
import no.nav.mulighetsrommet.model.Avtaletyper
import no.nav.mulighetsrommet.model.ProblemDetail
import no.nav.mulighetsrommet.model.Tiltakskode
import org.koin.java.KoinJavaComponent.inject
import org.koin.ktor.ext.inject

fun Route.kodeverkRoutes() {
    val opplaringKategorisering: OpplaringKategoriseringMapper by inject()
    val pamService: PamOntologiService by inject()

    route("kodeverk") {
        get("avtaletyper", {
            tags = setOf("Kodeverk")
            operationId = "getAvtaletyper"
            response {
                code(HttpStatusCode.OK) {
                    description = "Avtaletyper per tiltakskode"
                    body<Map<Tiltakskode, List<AvtaletypeInfo>>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val avtaletyper = Tiltakskode.entries.associateWith { tiltakskode ->
                Avtaletyper
                    .getAvtaletyperForTiltak(tiltakskode)
                    .map { AvtaletypeInfo(type = it, tittel = it.tittel) }
            }
            call.respond(avtaletyper)
        }

        get("prismodeller", {
            tags = setOf("Kodeverk")
            operationId = "getPrismodeller"
            response {
                code(HttpStatusCode.OK) {
                    description = "Prismodeller per tiltakskode"
                    body<Map<Tiltakskode, List<PrismodellInfo>>>()
                }
                default {
                    description = "Problem details"
                    body<ProblemDetail>()
                }
            }
        }) {
            val prismodeller = Tiltakskode.entries.associateWith { tiltakskode ->
                Prismodeller
                    .getPrismodellerForTiltak(tiltakskode)
                    .map { PrismodellInfo(type = it, navn = it.navn, beskrivelse = it.beskrivelse) }
            }
            call.respond(prismodeller)
        }

        route("opplaring") {
            get("kategorisering", {
                tags = setOf("Kodeverk")
                operationId = "getOpplaringKategorisering"
                description = "Struktur for kategorisering av arbeidsmarkedsopplæring"
                request {
                    queryParameter<Tiltakskode>("tiltakskode")
                }
                response {
                    code(HttpStatusCode.OK) {
                        description = "Kategorisering av kategorisering"
                        body<OpplaringKategoriseringResponse>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val tiltakskode = call.queryParameters.get("tiltakskode")?.let { Tiltakskode.valueOf(it) }
                if (tiltakskode == null) {
                    call.respond(BadRequest("Ukjent tiltakskode"))
                    return@get
                }
                val verk = opplaringKategorisering.from(tiltakskode)
                call.respond(verk)
            }

            get("/sertifiseringer/sok", {
                description = "Søk etter sertifiseringer"
                tags = setOf("Kodeverk")
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
                        body<List<Sertifisering>>()
                    }
                    default {
                        description = "Problem details"
                        body<ProblemDetail>()
                    }
                }
            }) {
                val q: String by call.request.queryParameters

                val sertifiseringer = pamService.sokSertifiseringer(q)

                call.respond(sertifiseringer)
            }
        }
    }
}
