package no.nav.mulighetsrommet.api.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import no.nav.mulighetsrommet.api.plugins.getNavIdent
import no.nav.mulighetsrommet.api.routes.v1.responses.PaginatedResponse
import no.nav.mulighetsrommet.notifications.NotificationService
import org.koin.ktor.ext.inject
import java.util.*

fun Route.notificationRoutes() {
    val notificationService: NotificationService by inject()

    route("api/v1/internal/notifications") {
        get {
            val userId = getNavIdent()

            val notifications = notificationService.getNotifications(userId)

            call.respond(PaginatedResponse.of(notifications))
        }

        post("{id}/seen") {
            val id = call.parameters.getOrFail<UUID>("id")
            val userId = getNavIdent()

            // TODO: access control to ensure that `id` is `userId`'s notificaiton
            notificationService.markNotificationAsSeen(id, userId)

            call.respond(HttpStatusCode.OK)
        }

        post("{id}/unseen") {
            val id = call.parameters.getOrFail<UUID>("id")
            val userId = getNavIdent()

            // TODO: access control to ensure that `id` is `userId`'s notificaiton
            notificationService.markNotificationAsUnseen(id, userId)

            call.respond(HttpStatusCode.OK)
        }
    }
}
