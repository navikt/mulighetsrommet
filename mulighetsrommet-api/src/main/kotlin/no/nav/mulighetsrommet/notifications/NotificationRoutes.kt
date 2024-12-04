package no.nav.mulighetsrommet.notifications

import arrow.core.getOrElse
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.ktor.exception.StatusException
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

fun Route.notificationRoutes() {
    val notificationRepository: NotificationRepository by inject()

    val logger = LoggerFactory.getLogger(javaClass)

    route("notifications") {
        get {
            val userId = getNavIdent()
            val filter = getNotificationFilter()

            val notifications = notificationRepository.getUserNotifications(userId, filter.status)
                .getOrElse {
                    logger.error("Failed to get notifications for user=$userId", it.error)
                    throw StatusException(InternalServerError, "Klarte ikke hente notifikasjoner")
                }

            call.respond(PaginatedResponse.of(notifications))
        }

        get("summary") {
            val userId = getNavIdent()

            val summary = notificationRepository.getUserNotificationSummary(userId)
                .getOrElse {
                    logger.error("Failed to get summary for user=$userId", it.error)
                    throw StatusException(InternalServerError, "Klarte ikke hente notifikasjoner")
                }

            call.respond(summary)
        }

        post("status") {
            val userId = getNavIdent()
            val body = call.receive<SetNotificationStatusRequest>()

            body.notifikasjoner.forEach {
                val doneAt = when (it.status) {
                    NotificationStatus.DONE -> LocalDateTime.now()
                    NotificationStatus.NOT_DONE -> null
                }
                notificationRepository.setNotificationDoneAt(it.id, userId, doneAt)
                    .onLeft {
                        logger.error("Failed to set notification status", it.error)
                        throw StatusException(InternalServerError, "Klarte ikke oppdatere notifikasjon med ny status")
                    }
                    .onRight { updated ->
                        if (updated == 0) {
                            throw StatusException(BadRequest, "Klarte ikke oppdatere notifikasjon med ny status")
                        }
                    }
            }

            call.respond(HttpStatusCode.OK)
        }
    }
}

@Serializable
data class SetNotificationStatusRequest(
    val notifikasjoner: List<StatusRequest>,
) {
    @Serializable
    data class StatusRequest(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val status: NotificationStatus,
    )
}

fun RoutingContext.getNotificationFilter(): NotificationFilter {
    val status = call.request.queryParameters["status"]
    return NotificationFilter(
        status = status?.let { NotificationStatus.valueOf(it) },
    )
}

data class NotificationFilter(
    val status: NotificationStatus? = null,
)
