package no.nav.mulighetsrommet.notifications

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.domain.dto.NavIdent
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*

class NotificationQueries(private val session: Session) {

    fun insert(notification: ScheduledNotification) {
        @Language("PostgreSQL")
        val insertNotification = """
            insert into notification (id, type, title, description, created_at, metadata)
            values (:id::uuid, :type::notification_type, :title, :description, :created_at, :metadata::jsonb)
            returning id, type, title, description, created_at, metadata::jsonb
        """.trimIndent()

        @Language("PostgreSQL")
        val insertUserNotification = """
            insert into user_notification (notification_id, user_id, done_at)
            values (:notification_id::uuid, :user_id, :done_at)
            returning notification_id, user_id, done_at
        """.trimIndent()

        val notificationParams = mapOf(
            "id" to notification.id,
            "type" to notification.type.name,
            "title" to notification.title,
            "description" to notification.description,
            "created_at" to notification.createdAt,
            "metadata" to notification.metadata?.let { Json.encodeToString(it) },
        )
        session.execute(queryOf(insertNotification, notificationParams))

        val targets = notification.targets.map {
            mapOf(
                "notification_id" to notification.id,
                "user_id" to it.value,
                "done_at" to null,
            )
        }
        session.batchPreparedNamedStatement(insertUserNotification, targets)
    }

    fun setNotificationDoneAt(id: UUID, userId: NavIdent, doneAt: LocalDateTime?): Int {
        @Language("PostgreSQL")
        val query = """
            with matched_notification as (select *
                                          from notification n
                                               join user_notification un on n.id = un.notification_id
                                              and notification_id = :notification_id
                                              and user_id = :user_id)
            update user_notification un
            set done_at = :done_at
            from matched_notification mn
            where un.notification_id = mn.id and (un.user_id = :user_id or mn.type = 'TASK')
        """.trimIndent()

        val params = mapOf("notification_id" to id, "user_id" to userId.value, "done_at" to doneAt)

        return session.update(queryOf(query, params))
    }

    fun get(id: UUID): UserNotification {
        @Language("PostgreSQL")
        val query = """
            select n.id, n.type, n.title, n.description, n.created_at, un.user_id, un.done_at, n.metadata
            from notification n
                     left join user_notification un on n.id = un.notification_id
            where id = ?::uuid
        """.trimIndent()

        val notification = session.single(queryOf(query, id)) { it.toUserNotification() }

        return requireNotNull(notification)
    }

    fun getAll(): List<UserNotification> {
        @Language("PostgreSQL")
        val query = """
            select n.id, n.type, n.title, n.description, n.created_at, un.user_id, un.done_at, n.metadata
            from notification n
                     left join user_notification un on n.id = un.notification_id
            order by created_at desc
        """.trimIndent()

        return session.list(queryOf(query)) { it.toUserNotification() }
    }

    fun getUserNotifications(
        userId: NavIdent? = null,
        status: NotificationStatus? = null,
    ): List<UserNotification> {
        @Language("PostgreSQL")
        val query = """
            select n.id, n.type, n.title, n.description, n.created_at, un.user_id, un.done_at, n.metadata
            from notification n
                     left join user_notification un on n.id = un.notification_id
            where (:user_id::text is null or un.user_id = :user_id)
              and (:status::text is null
                    or (:status = 'DONE' and un.done_at is not null)
                    or (:status = 'NOT_DONE' and un.done_at is null))
            order by created_at desc
        """.trimIndent()

        val params = mapOf("user_id" to userId?.value, "status" to status?.name)

        return session.list(queryOf(query, params)) { it.toUserNotification() }
    }

    fun getUserNotificationSummary(userId: NavIdent): UserNotificationSummary {
        @Language("PostgreSQL")
        val query = """
            select count(*) as not_done_count
            from notification n
                     join user_notification un on n.id = un.notification_id
            where user_id = ?
              and done_at is null
        """.trimIndent()

        val summary = session.single(queryOf(query, userId.value)) {
            UserNotificationSummary(it.int("not_done_count"))
        }
        return requireNotNull(summary)
    }

    fun delete(id: UUID): Int {
        @Language("PostgreSQL")
        val query = """
            delete from notification
            where id = ?::uuid
        """.trimIndent()

        return session.update(queryOf(query, id))
    }
}

private fun Row.toUserNotification() = UserNotification(
    id = uuid("id"),
    type = NotificationType.valueOf(string("type")),
    title = string("title"),
    description = stringOrNull("description"),
    user = NavIdent(string("user_id")),
    createdAt = localDateTime("created_at"),
    doneAt = localDateTimeOrNull("done_at"),
    metadata = stringOrNull("metadata")?.let { Json.decodeFromString(it) },
)
