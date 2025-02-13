package no.nav.tiltak.historikk

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.arena.ArenaDeltakerDbo
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.kafka.Topic
import no.nav.mulighetsrommet.model.TiltakshistorikkRequest
import no.nav.tiltak.historikk.repositories.DeltakerRepository
import java.util.*

fun Route.tiltakshistorikkRoutes(
    kafka: KafkaConsumerOrchestrator,
    deltakerRepository: DeltakerRepository,
    service: TiltakshistorikkService,
) {
    authenticate {
        route("/api/v1/historikk") {
            post {
                val request = call.receive<TiltakshistorikkRequest>()

                val response = service.getTiltakshistorikk(request)

                call.respond(response)
            }
        }

        route("/api/v1/intern/arena") {
            put("/deltaker") {
                val dbo = call.receive<ArenaDeltakerDbo>()

                deltakerRepository.upsertArenaDeltaker(dbo)

                call.respond(HttpStatusCode.OK)
            }

            delete("/deltaker/{id}") {
                val id: UUID by call.parameters

                deltakerRepository.deleteArenaDeltaker(id)

                call.respond(HttpStatusCode.OK)
            }
        }
    }

    authenticate {
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
