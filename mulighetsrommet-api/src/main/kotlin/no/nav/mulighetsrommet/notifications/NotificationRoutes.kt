package no.nav.mulighetsrommet.notifications

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.time.LocalDateTime
import java.util.*

fun Route.notificationRoutes() {
    val db: ApiDatabase by inject()

    route("notifications") {
        get {
            val userId = getNavIdent()
            val filter = getNotificationFilter()

            val notifications = db.session {
                queries.notifications.getUserNotifications(userId, filter.status)
            }

            call.respond(PaginatedResponse.of(notifications))
        }

        get("summary") {
            val userId = getNavIdent()

            val summary = db.session {
                queries.notifications.getUserNotificationSummary(userId)
            }

            call.respond(summary)
        }

        post("status") {
            val userId = getNavIdent()
            val (notifikasjoner) = call.receive<SetNotificationStatusRequest>()

            db.transaction {
                notifikasjoner.forEach {
                    val readAt = when (it.status) {
                        NotificationStatus.READ -> LocalDateTime.now()
                        NotificationStatus.UNREAD -> null
                    }
                    queries.notifications.setNotificationReadAt(it.id, userId, readAt)
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
