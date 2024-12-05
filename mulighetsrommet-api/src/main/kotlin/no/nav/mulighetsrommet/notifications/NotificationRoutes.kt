package no.nav.mulighetsrommet.notifications

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.responses.PaginatedResponse
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import org.koin.ktor.ext.inject
import java.util.*

fun Route.notificationRoutes() {
    val notificationService: NotificationService by inject()

    route("notifications") {
        get {
            val userId = getNavIdent()
            val filter = getNotificationFilter()

            val notifications = notificationService.getNotifications(userId, filter)

            call.respond(PaginatedResponse.of(notifications))
        }

        get("summary") {
            val userId = getNavIdent()

            val summary = notificationService.getNotificationSummary(userId)

            call.respond(summary)
        }

        post("status") {
            val userId = getNavIdent()
            val body = call.receive<SetNotificationStatusRequest>()

            body.notifikasjoner.forEach {
                notificationService.setNotificationStatus(it.id, userId, it.status)
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
