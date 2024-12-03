package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.Task
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import kotliquery.Session
import no.nav.mulighetsrommet.api.gjennomforing.task.InitialLoadTiltaksgjennomforinger
import no.nav.mulighetsrommet.api.navansatt.task.SynchronizeNavAnsatte
import no.nav.mulighetsrommet.api.refusjon.task.GenerateRefusjonskrav
import no.nav.mulighetsrommet.api.refusjon.task.JournalforRefusjonskrav
import no.nav.mulighetsrommet.api.tiltakstype.task.InitialLoadTiltakstyper
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.notifications.ScheduleNotification
import no.nav.mulighetsrommet.notifications.ScheduledNotification
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import no.nav.mulighetsrommet.utdanning.task.SynchronizeUtdanninger
import java.io.PrintWriter
import java.sql.Connection
import java.time.Instant
import java.util.*
import javax.sql.DataSource

class DbSchedulerClient(
    private val database: Database,
    private val journalforRefusjonskrav: JournalforRefusjonskrav,
    private val initialLoadTiltaksgjennomforinger: InitialLoadTiltaksgjennomforinger,
    private val initialLoadTiltakstyper: InitialLoadTiltakstyper,
    private val generateValidationReport: GenerateValidationReport,
    private val synchronizeNavAnsatte: SynchronizeNavAnsatte,
    private val synchronizeUtdanninger: SynchronizeUtdanninger,
    private val generateRefusjonskrav: GenerateRefusjonskrav,
    private val scheduleNotification: ScheduleNotification,
) {
    fun scheduleJournalforRefusjonskrav(input: JournalforRefusjonskrav.TaskInput, startTime: Instant, tx: Session): String = scheduleIfNotExists(journalforRefusjonskrav.task, input, startTime, input.refusjonskravId, tx)

    fun scheduleInitialLoadTiltaksgjennomforinger(input: InitialLoadTiltaksgjennomforinger.TaskInput, startTime: Instant): String = database.transaction { tx ->
        scheduleIfNotExists(initialLoadTiltaksgjennomforinger.task, input, startTime, UUID.randomUUID(), tx)
    }

    fun scheduleInitialLoadTiltakstyper(startTime: Instant): String = database.transaction { tx ->
        scheduleIfNotExists(initialLoadTiltakstyper.task, null, startTime, UUID.randomUUID(), tx)
    }

    fun scheduleGenerateValidationReport(startTime: Instant): String = database.transaction { tx ->
        scheduleIfNotExists(generateValidationReport.task, null, startTime, UUID.randomUUID(), tx)
    }

    fun scheduleNotification(notification: ScheduledNotification, startTime: Instant): String = database.transaction { tx ->
        scheduleIfNotExists(scheduleNotification.task, notification, startTime, notification.id, tx)
    }

    fun scheduleSynchronizeNavAnsatte(startTime: Instant): String = database.transaction { tx ->
        reschedule(synchronizeNavAnsatte.task, null, startTime, tx)
    }

    fun scheduleSynchronizeUtdanninger(startTime: Instant): String = database.transaction { tx ->
        reschedule(synchronizeUtdanninger.task, null, startTime, tx)
    }

    fun scheduleGenerateRefusjonskrav(input: GenerateRefusjonskrav.TaskInput, startTime: Instant): String = database.transaction { tx ->
        reschedule(generateRefusjonskrav.task, input, startTime, tx)
    }

    private fun <T> scheduleIfNotExists(
        task: Task<T>,
        input: T?,
        startTime: Instant,
        id: UUID,
        tx: Session,
    ): String {
        val client = getClient(task, tx)

        val instance = task.instance(id.toString(), input)
        client.scheduleIfNotExists(instance, startTime)
        return id.toString()
    }

    private fun <T> reschedule(task: RecurringTask<T>, input: T?, startTime: Instant): String = database.transaction { tx ->
        reschedule(task, input, startTime, tx)
    }

    private fun <T> reschedule(task: RecurringTask<T>, input: T?, startTime: Instant, tx: Session): String {
        val existingTaskId = task.defaultTaskInstance.id
        val client = getClient(task, tx)
        val existingSchedule = client.getScheduledExecution(task.instance(existingTaskId)).get()

        if (existingSchedule.isPicked) {
            throw IllegalArgumentException("Task already running.")
        }

        client.reschedule(task.instance(existingTaskId, input), startTime)
        return existingTaskId
    }

    private fun <T> getClient(task: Task<T>, tx: Session) = SchedulerClient.Builder
        .create(TransactionalSessionDataSource(tx), task)
        .serializer(DbSchedulerKotlinSerializer())
        .build()
}

class TransactionalSessionDataSource(private val session: Session) : DataSource {
    override fun getConnection(): Connection {
        return UncloseableConnection(session.connection.underlying)
    }

    override fun getConnection(username: String?, password: String?): Connection {
        throw UnsupportedOperationException("Connection with username and password is not supported.")
    }

    override fun getLogWriter(): PrintWriter? {
        return null // Optional: Implement if needed
    }

    override fun setLogWriter(out: PrintWriter?) {
        throw UnsupportedOperationException("LogWriter is not supported.")
    }

    override fun getLoginTimeout(): Int {
        return 0 // Optional: Implement if needed
    }

    override fun setLoginTimeout(seconds: Int) {
        throw UnsupportedOperationException("LoginTimeout is not supported.")
    }

    override fun getParentLogger(): java.util.logging.Logger {
        throw UnsupportedOperationException("ParentLogger is not supported.")
    }

    override fun <T : Any?> unwrap(iface: Class<T>?): T {
        throw UnsupportedOperationException("Unsupported unwrap operation.")
    }

    override fun isWrapperFor(iface: Class<*>?): Boolean {
        return false
    }
}

class UncloseableConnection(private val underlying: Connection) : Connection by underlying {
    override fun close() {
        // Do nothing
    }
}
