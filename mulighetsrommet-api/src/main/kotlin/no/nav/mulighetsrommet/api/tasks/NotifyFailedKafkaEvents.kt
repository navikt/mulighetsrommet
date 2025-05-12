package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.DisabledSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import no.nav.mulighetsrommet.kafka.KafkaConsumerOrchestrator
import no.nav.mulighetsrommet.slack.SlackNotifier
import no.nav.mulighetsrommet.tasks.executeSuspend

class NotifyFailedKafkaEvents(
    private val config: Config,
    private val kafkaConsumerOrchestrator: KafkaConsumerOrchestrator,
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
        .executeSuspend { _, _ ->
            val retries = config.maxRetries
            val failedEvents = kafkaConsumerOrchestrator.getAllStoredConsumerRecords()
            val topicCounts = failedEvents
                .groupBy { it.topic }
                .map { "${it.key} : ${it.value.count()}" }
                .joinToString()

            if (failedEvents.isNotEmpty()) {
                val message = """
                    Det finnes ${failedEvents.size} rader i tabellen 'kafka_consumer_record' som har retries >= $retries. Count per topic: $topicCounts
                """.trimIndent()
                slackNotifier.sendMessage(message)
            }
        }
}
