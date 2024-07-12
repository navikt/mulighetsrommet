package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.routes.v1.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.utils.getNotificationFilter
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import no.nav.mulighetsrommet.notifications.NotificationService
import no.nav.mulighetsrommet.notifications.NotificationStatus
import org.koin.ktor.ext.inject
import java.util.*

fun Route.notificationRoutes() {
    val notificationService: NotificationService by inject()

    route("api/v1/intern/notifications") {
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
