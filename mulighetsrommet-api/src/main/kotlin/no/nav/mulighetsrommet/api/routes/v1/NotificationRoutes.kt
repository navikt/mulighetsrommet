package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.routes.v1.responses.PaginatedResponse
import no.nav.mulighetsrommet.api.utils.getNotificationFilter
import no.nav.mulighetsrommet.notifications.NotificationService
import org.koin.ktor.ext.inject
import java.util.*

fun Route.notificationRoutes() {
    val notificationService: NotificationService by inject()

    route("api/v1/internal/notifications") {
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

        post("{id}/read") {
            val id = call.parameters.getOrFail<UUID>("id")
            val userId = getNavIdent()

            notificationService.markNotificationAsRead(id, userId)

            call.respond(HttpStatusCode.OK)
        }

        post("{id}/unread") {
            val id = call.parameters.getOrFail<UUID>("id")
            val userId = getNavIdent()

            notificationService.markNotificationAsUnread(id, userId)

            call.respond(HttpStatusCode.OK)
        }
    }
}
