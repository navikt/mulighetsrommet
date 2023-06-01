package no.nav.mulighetsrommet.api.tasks

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay
import kotlinx.coroutines.runBlocking
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.kafka.KafkaConsumerRepository
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory

class NotifyFailedKafkaEvents(
    private val config: Config,
    val database: Database,
    private val kafkaConsumerRepository: KafkaConsumerRepository,
    private val slackNotifier: SlackNotifier,
) {

    data class Config(
        val delayOfMinutes: Int,
        val maxRetries: Int,
    )

    private val logger = LoggerFactory.getLogger(javaClass)
    private val taskName = "notify-failed-kafka-events"

    val task: RecurringTask<Void> = Tasks
        .recurring(taskName, FixedDelay.ofMinutes(config.delayOfMinutes))
        .onFailure { _, _ ->
            slackNotifier.sendMessage("Klarte ikke kjøre task '$taskName'. Konsekvensen er at man ikke får gitt beskjed på Slack dersom det finnes kafka events som har failed etter for mange retries.")
        }
        .execute { instance, _ ->
            logger.info("Running task ${instance.taskName}")

            runBlocking {
                val retries = config.maxRetries
                val failedEvents = kafkaConsumerRepository.getAllRecords()
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
