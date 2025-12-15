package no.nav.tiltak.historikk

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.util.getValue
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.Topic
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.plugins.TiltakshistorikkAdmin
import no.nav.tiltak.historikk.plugins.TiltakshistorikkRead
import no.nav.tiltak.historikk.plugins.TiltakshistorikkWrite
import no.nav.tiltak.historikk.plugins.authenticate
import no.nav.tiltak.historikk.service.TiltakshistorikkService
import no.nav.tiltak.historikk.service.VirksomhetService
import java.util.UUID

fun Route.tiltakshistorikkRoutes(
    kafka: KafkaConsumerOrchestrator,
    db: TiltakshistorikkDatabase,
    tiltakshistorikk: TiltakshistorikkService,
    virksomheter: VirksomhetService,
) {
    authenticate(TiltakshistorikkRead) {
        route("/api/v1/historikk") {
            post {
                val request = call.receive<TiltakshistorikkV1Request>()

                val response: TiltakshistorikkV1Response = tiltakshistorikk.getTiltakshistorikk(request)

                call.respond(response)
            }
        }
    }

    authenticate(TiltakshistorikkWrite) {
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

    authenticate(TiltakshistorikkAdmin) {
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
}
