package no.nav.mulighetsrommet.kafka

import no.nav.common.kafka.consumer.feilhandtering.StoredConsumerRecord
import no.nav.common.kafka.consumer.feilhandtering.backoff.BackoffStrategy
import no.nav.common.kafka.consumer.feilhandtering.backoff.LinearBackoffStrategy
import no.nav.common.kafka.util.KafkaUtils
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.hours

/**
 * Standard strategi er at consumer records blir forsøkt prosessert hvert 5 minutt med et tak på 12 timer.
 */
val LinearBackoffStrategy = run {
    val minBackoffSeconds = 0
    val maxBackoffSeconds = 12.hours.inWholeSeconds.toInt()
    val maxBackoffAfterRetries = 144
    LinearBackoffStrategy(minBackoffSeconds, maxBackoffSeconds, maxBackoffAfterRetries)
}

const val KAFKA_CONSUMER_RECORD_PROCESSOR_SCHEDULED_AT = "kcrp-scheduled-at"

/**
 * Denne strategien kan brukes når du ønsker å skedulere en Kafka-melding til prosessering på et bestemt tidspunkt.
 * For å ta den i bruk må headeren [KAFKA_CONSUMER_RECORD_PROCESSOR_SCHEDULED_AT] settes til en verdi av typen [java.time.Instant],
 * og consumeren må være en instans av [ScheduledMessageKafkaTopicConsumer].
 */
object WaitUntilScheduledAtBackoffStrategy : BackoffStrategy {
    override fun getBackoffDuration(record: StoredConsumerRecord): Duration {
        val headers = KafkaUtils.jsonToHeaders(record.headersJson)

        val duration = headers.firstOrNull { it.key() == KAFKA_CONSUMER_RECORD_PROCESSOR_SCHEDULED_AT }?.let { ts ->
            val scheduledAt = Instant.parse(String(ts.value()))
            val lastAttempt = getLastAttempt(record)
            Duration.between(lastAttempt, scheduledAt).takeIf { it > Duration.ZERO }
        }

        return duration ?: LinearBackoffStrategy.getBackoffDuration(record)
    }
}

private fun getLastAttempt(record: StoredConsumerRecord): Instant {
    return if (record.lastRetry != null) {
        record.lastRetry.toInstant()
    } else {
        Instant.ofEpochMilli(record.timestamp)
    }
}
