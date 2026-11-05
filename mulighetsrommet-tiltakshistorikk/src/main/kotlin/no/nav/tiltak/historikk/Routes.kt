package no.nav.tiltak.historikk

import io.ktor.http.HttpStatusCode
import io.ktor.openapi.OpenApiDoc
import io.ktor.openapi.OpenApiInfo
import io.ktor.openapi.jsonSchema
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.openapi.hide
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import io.ktor.utils.io.ExperimentalKtorApi
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.Topic
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.plugins.AuthProvider
import no.nav.tiltak.historikk.plugins.authenticate
import no.nav.tiltak.historikk.service.TiltakshistorikkService
import no.nav.tiltak.historikk.service.VirksomhetService
import java.util.UUID

@OptIn(ExperimentalKtorApi::class)
fun Route.tiltakshistorikkRoutes(
    kafka: KafkaConsumerOrchestrator,
    db: TiltakshistorikkDatabase,
    tiltakshistorikk: TiltakshistorikkService,
    virksomheter: VirksomhetService,
) {
    val api = authenticate(AuthProvider.TILTAKSHISTORIKK_READ) {
        route("/api/v1/historikk") {
            post {
                val request = call.receive<TiltakshistorikkV1Request>()

                val response: TiltakshistorikkV1Response = tiltakshistorikk.getTiltakshistorikk(request)

                call.respond(response)
            }.describe {
                summary = "Hent tiltakshistorikk"
                description = """
                    Henter tiltakshistorikk for ett eller flere fødselsnummer oppgitt i `identer`.
                    Resultatet er en samlet liste med deltakelser fra Arena, tiltak hos arrangør fra Team Komet
                    og tiltak hos arbeidsgiver fra Team Tiltak, der feltet `type` angir deltakelsestypen.
                    For komplett historikk per bruker så må konsumenten hente alle historiske identer fra PDL
                    og inkludere dem i forespørselen.
                """.trimIndent()
                requestBody {
                    required = true
                    schema = jsonSchema<TiltakshistorikkV1Request>()
                }
                responses {
                    HttpStatusCode.OK {
                        description = "Tiltakshistorikk for de oppgitte identene."
                        schema = jsonSchema<TiltakshistorikkV1Response>()
                    }
                    HttpStatusCode.Unauthorized {
                        description = "Mangler gyldig tilgang."
                    }
                }
            }
        }
    }

    authenticate(AuthProvider.TILTAKSHISTORIKK_WRITE) {
        route("/api/v1/intern/arena") {
            put("/gjennomforing") {
                val dbo = call.receive<TiltakshistorikkArenaGjennomforing>()

                virksomheter.getOrSyncVirksomhetIfNotExists(dbo.arrangorOrganisasjonsnummer)
                    .onRight {
                        db.session { queries.arenaGjennomforing.upsert(dbo) }
                        call.respond(HttpStatusCode.OK)
                    }
                    .onLeft {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "Klarte ikke utlede virksomhet fra for orgnr=${dbo.arrangorOrganisasjonsnummer}",
                        )
                    }
            }

            delete("/gjennomforing/{id}") {
                val id: UUID by call.parameters

                db.session { queries.arenaGjennomforing.delete(id) }

                call.respond(HttpStatusCode.OK)
            }

            put("/deltaker") {
                val dbo = call.receive<TiltakshistorikkArenaDeltaker>()

                db.session { queries.arenaDeltaker.upsertArenaDeltaker(dbo) }

                call.respond(HttpStatusCode.OK)
            }

            delete("/deltaker/{id}") {
                val id: UUID by call.parameters

                db.session { queries.arenaDeltaker.deleteArenaDeltaker(id) }

                call.respond(HttpStatusCode.OK)
            }
        }
    }

    authenticate(AuthProvider.TEAM_MULIGHETSROMMET) {
        route("/maam") {
            route("/topics") {
                get {
                    val topics = kafka.getTopics()
                    call.respond(topics)
                }

                put {
                    val topics = call.receive<List<Topic>>()
                    kafka.updateRunningTopics(topics)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }

    val source = OpenApiDocSource.Routing(
        routes = { api.descendants() },
    )
    get("/openapi.json") {
        val document = source.read(
            call.application,
            OpenApiDoc(info = OpenApiInfo("Tiltakshistorikk", "v1")),
        )
        call.respondText(document.content, document.contentType)
    }.hide()
}
