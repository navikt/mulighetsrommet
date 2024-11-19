package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.kafka.KafkaConsumerRepositoryImpl
import no.nav.mulighetsrommet.slack.SlackNotifier

class NotifyFailedKafkaEvents(
    private val config: Config,
    val database: Database,
    private val kafkaConsumerRepository: KafkaConsumerRepositoryImpl,
    private val slackNotifier: SlackNotifier,
) {

    data class Config(
        val disabled: Boolean = false,
        val cronPattern: String,
        val maxRetries: Int,
    ) {
        fun toSchedule(): Schedule {
            return if (disabled) {
                DisabledSchedule()
            } else {
                Schedules.cron(cronPattern)
            }
        }
    }

    val task: RecurringTask<Void> = Tasks
        .recurring(javaClass.simpleName, config.toSchedule())
        .execute { _, _ ->
            runBlocking {
                val retries = config.maxRetries
                val failedEvents = kafkaConsumerRepository.getAll()
                val topicCounts = failedEvents
                    .groupBy { it.topic }
                    .map { "${it.key} : ${it.value.count()}" }
                    .joinToString()

                if (failedEvents.isNotEmpty()) {
                    val message = """
                    Det finnes ${failedEvents.size} rader i tabellen 'failed_events' som har retries >= $retries. Count per topic: $topicCounts
                    """.trimIndent()
                    slackNotifier.sendMessage(message)
                }
            }
        }
}
