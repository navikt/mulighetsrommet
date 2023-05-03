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

    fun upsert(notification: Notification): QueryResult<Notification> = query {
        logger.info("Saving notification id=${notification.id}")

        @Language("PostgreSQL")
        val upsertNotification = """
            insert into notification (id, type, target, title, description, created_at)
            values (:id::uuid, :type::notification_type, :target::notification_target, :title, :description, :created_at)
            on conflict (id) do update set type        = excluded.type,
                                           target      = excluded.target,
                                           title       = excluded.title,
                                           description = excluded.description
            returning id, type, target, title, description, created_at
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertUserNotification = """
            insert into user_notification (notification_id, user_id, read_at)
            values (:notification_id::uuid, :user_id, :read_at)
            on conflict (notification_id, user_id) do update set read_at = excluded.read_at
            returning notification_id, user_id, read_at
        """.trimIndent()

        db.transaction { tx ->
            val notificationDbo = queryOf(
                upsertNotification,
                mapOf<String, Any?>(
                    "id" to notification.id,
                    "type" to notification.type.name,
                    "target" to if (notification.user == null) {
                        NotificationTarget.All
                    } else {
                        NotificationTarget.User
                    }.name,
                    "title" to notification.title,
                    "description" to notification.description,
                    "created_at" to notification.createdAt,
                ),
            )
                .map { it.toNotificationDbo() }
                .asSingle
                .let { tx.run(it)!! }

            val userNotificationDbo = notification.user?.let {
                queryOf(
                    upsertUserNotification,
                    mapOf(
                        "notification_id" to notification.id,
                        "user_id" to notification.user,
                        "read_at" to null,
                    ),
                )
                    .map { it.toUserNotificationDbo() }
                    .asSingle
                    .let { tx.run(it)!! }
            }

            Notification(
                id = notificationDbo.id,
                type = notificationDbo.type,
                title = notificationDbo.title,
                description = notificationDbo.description,
                user = userNotificationDbo?.userId,
                createdAt = notificationDbo.createdAt,
            )
        }
    }

    fun setNotificationReadAt(id: UUID, userId: String, readAt: LocalDateTime?): QueryResult<Unit> = query {
        logger.info("Setting notification id=$id readAt=$readAt")

        @Language("PostgreSQL")
        val query = """
            insert into user_notification (notification_id, user_id, read_at)
            values (:notification_id::uuid, :user_id, :read_at)
            on conflict (notification_id, user_id) do update set read_at = excluded.read_at
            returning notification_id, user_id, read_at
        """.trimIndent()

        queryOf(query, mapOf("notification_id" to id, "user_id" to userId, "read_at" to readAt))
            .asExecute
            .let { db.run(it) }
    }

    fun get(id: UUID): QueryResult<Notification> = query {
        logger.info("Getting notification id=$id")

        @Language("PostgreSQL")
        val query = """
            select n.id, n.type, n.title, n.description, n.created_at, un.user_id
            from notification n
                     left join user_notification un on n.id = un.notification_id
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .map { it.toNotification() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun getAll(): QueryResult<List<Notification>> = query {
        @Language("PostgreSQL")
        val query = """
            select n.id, n.type, n.title, n.description, n.created_at, un.user_id
            from notification n
                     left join user_notification un on n.id = un.notification_id
            order by created_at desc
        """.trimIndent()

        queryOf(query)
            .map { it.toNotification() }
            .asList
            .let { db.run(it) }
    }

    fun getUserNotifications(userId: String? = null): QueryResult<List<UserNotification>> = query {
        val where = DatabaseUtils.andWhereParameterNotNull(
            userId to "n.target = 'All' or un.user_id = :user_id",
        )

        @Language("PostgreSQL")
        val query = """
            select n.id, n.type, n.target, n.title, n.description, n.created_at, un.user_id, un.read_at
            from notification n
                     left join user_notification un on n.id = un.notification_id
            $where
            order by created_at desc
        """.trimIndent()

        queryOf(query, mapOf("user_id" to userId))
            .map { it.toUserNotification(userId) }
            .asList
            .let { db.run(it) }
    }

    fun getUserNotificationSummary(userId: String): QueryResult<UserNotificationSummary> = query {
        @Language("PostgreSQL")
        val query = """
            with all_common_notifications as (select count(*) as count
                                              from notification
                                              where target = 'All'),
                 read_common_notifications as (select count(*) as count
                                               from notification n
                                                        left join user_notification un on n.id = un.notification_id
                                               where target = 'All'
                                                 and (un.user_id = :user_id and un.read_at is not null)),
                 unread_user_notifications as (select count(*) as count
                                               from notification n
                                                        join user_notification un on n.id = un.notification_id
                                               where n.target = 'User'
                                                 and user_id = :user_id
                                                 and read_at is null)
            select (select count from all_common_notifications) -
                   (select count from read_common_notifications) +
                   (select count from unread_user_notifications) as unread_count
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

    private fun Row.toNotificationDbo() = NotificationDbo(
        id = uuid("id"),
        type = NotificationType.valueOf(string("type")),
        target = NotificationTarget.valueOf(string("target")),
        title = string("title"),
        description = stringOrNull("description"),
        createdAt = instant("created_at"),
    )

    private fun Row.toUserNotificationDbo() = UserNotificationDbo(
        notificationId = uuid("notification_id"),
        userId = string("user_id"),
        readAt = localDateTimeOrNull("read_at"),
    )

    private fun Row.toNotification() = Notification(
        id = uuid("id"),
        type = NotificationType.valueOf(string("type")),
        title = string("title"),
        description = stringOrNull("description"),
        user = stringOrNull("user_id"),
        createdAt = instant("created_at"),
    )

    private fun Row.toUserNotification(userId: String?) = UserNotification(
        id = uuid("id"),
        type = NotificationType.valueOf(string("type")),
        title = string("title"),
        description = stringOrNull("description"),
        user = userId ?: string("user_id"),
        createdAt = localDateTime("created_at"),
        readAt = localDateTimeOrNull("read_at"),
    )
}
