package no.nav.mulighetsrommet.notifications

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.utils.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class NotificationRepository(private val db: Database) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun insert(notification: ScheduledNotification): QueryResult<Unit> = query {
        logger.info("Saving notification id=${notification.id}")

        @Language("PostgreSQL")
        val insertNotification = """
            insert into notification (id, type, title, description, created_at)
            values (:id::uuid, :type::notification_type, :title, :description, :created_at)
            returning id, type, title, description, created_at
        """.trimIndent()

        @Language("PostgreSQL")
        val insertUserNotification = """
            insert into user_notification (notification_id, user_id, read_at)
            values (:notification_id::uuid, :user_id, :read_at)
            returning notification_id, user_id, read_at
        """.trimIndent()

        db.transaction { tx ->
            queryOf(
                insertNotification,
                mapOf(
                    "id" to notification.id,
                    "type" to notification.type.name,
                    "title" to notification.title,
                    "description" to notification.description,
                    "created_at" to notification.createdAt,
                ),
            )
                .asExecute
                .let { tx.run(it) }

            notification.targets.forEach { userId ->
                queryOf(
                    insertUserNotification,
                    mapOf(
                        "notification_id" to notification.id,
                        "user_id" to userId,
                        "read_at" to null,
                    ),
                )
                    .asExecute
                    .let { tx.run(it) }
            }
        }
    }

    fun setNotificationReadAt(id: UUID, userId: String, readAt: LocalDateTime?): QueryResult<Int> = query {
        logger.info("Setting notification id=$id readAt=$readAt")

        @Language("PostgreSQL")
        val query = """
            update user_notification
            set read_at = :read_at
            where notification_id = :notification_id and user_id = :user_id
        """.trimIndent()

        queryOf(query, mapOf("notification_id" to id, "user_id" to userId, "read_at" to readAt))
            .asUpdate
            .let { db.run(it) }
    }

    fun get(id: UUID): QueryResult<UserNotification> = query {
        logger.info("Getting notification id=$id")

        @Language("PostgreSQL")
        val query = """
            select n.id, n.type, n.title, n.description, n.created_at, un.user_id, un.read_at
            from notification n
                     left join user_notification un on n.id = un.notification_id
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .map { it.toUserNotification() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun getAll(): QueryResult<List<UserNotification>> = query {
        @Language("PostgreSQL")
        val query = """
            select n.id, n.type, n.title, n.description, n.created_at, un.user_id, un.read_at
            from notification n
                     left join user_notification un on n.id = un.notification_id
            order by created_at desc
        """.trimIndent()

        queryOf(query)
            .map { it.toUserNotification() }
            .asList
            .let { db.run(it) }
    }

    fun getUserNotifications(userId: String? = null, filter: NotificationFilter): QueryResult<List<UserNotification>> =
        query {
            val where = DatabaseUtils.andWhereParameterNotNull(
                userId to "un.user_id = :user_id",
                filter.status to filter.status?.toDbStatement(),
            )

            @Language("PostgreSQL")
            val query = """
            select n.id, n.type, n.title, n.description, n.created_at, un.user_id, un.read_at
            from notification n
                     left join user_notification un on n.id = un.notification_id
            $where
            order by created_at desc
            """.trimIndent()

            queryOf(query, mapOf("user_id" to userId))
                .map { it.toUserNotification() }
                .asList
                .let { db.run(it) }
        }

    fun getUserNotificationSummary(userId: String): QueryResult<UserNotificationSummary> = query {
        @Language("PostgreSQL")
        val query = """
            select count(*) as unread_count
            from notification n
                     join user_notification un on n.id = un.notification_id
            where user_id = :user_id
              and read_at is null
        """.trimIndent()

        queryOf(query, mapOf("user_id" to userId))
            .map {
                UserNotificationSummary(
                    unreadCount = it.int("unread_count"),
                )
            }
            .asSingle
            .let { db.run(it)!! }
    }

    fun delete(id: UUID): QueryResult<Int> = query {
        logger.info("Deleting notification id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from notification
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asUpdate
            .let { db.run(it) }
    }

    private fun Row.toUserNotification() = UserNotification(
        id = uuid("id"),
        type = NotificationType.valueOf(string("type")),
        title = string("title"),
        description = stringOrNull("description"),
        user = string("user_id"),
        createdAt = localDateTime("created_at"),
        readAt = localDateTimeOrNull("read_at"),
    )

    private fun NotificationStatus.toDbStatement(): String {
        return when (this) {
            NotificationStatus.Read -> "un.read_at is not null"
            NotificationStatus.Unread -> "un.read_at is null"
        }
    }
}
