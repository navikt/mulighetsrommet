package no.nav.mulighetsrommet.kafka.monitoring

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.requireSingle
import org.intellij.lang.annotations.Language

class KafkaMetrics(
    private val database: Database,
) {
    private val metricsRegistrations = mutableListOf<(MeterRegistry) -> Unit>()

    fun withCountStaleConsumerRecords(minutesSinceCreatedAt: Int): KafkaMetrics {
        metricsRegistrations.add { registry ->
            Gauge.builder("kafka_consumer_records_stale_count") { countConsumerRecords(minutesSinceCreatedAt) }
                .description("Number of stale records in the kafka_consumer_record table")
                .register(registry)
        }

        return this
    }

    fun withCountStaleProducerRecords(minutesSinceCreatedAt: Int): KafkaMetrics {
        metricsRegistrations.add { registry ->
            Gauge.builder("kafka_producer_records_stale_count") { countProducerRecords(minutesSinceCreatedAt) }
                .description("Number of stale records in the kafka_producer_record table")
                .register(registry)
        }

        return this
    }

    fun register(registry: MeterRegistry) {
        metricsRegistrations.forEach { registerMetric ->
            registerMetric(registry)
        }
    }

    private fun countConsumerRecords(minutesSinceCreatedAt: Int): Int = database.session {
        @Language("PostgreSQL")
        val query = """
            select count(*) as count
            from kafka_consumer_record
            where created_at < now() - make_interval(mins := ?)
        """.trimIndent()

        it.requireSingle(queryOf(query, minutesSinceCreatedAt)) {
            it.int("count")
        }
    }

    private fun countProducerRecords(minutesSinceCreatedAt: Int): Int = database.session {
        @Language("PostgreSQL")
        val query = """
            select count(*) as count
            from kafka_producer_record
            where created_at < now() - make_interval(mins := ?)
        """.trimIndent()

        it.requireSingle(queryOf(query, minutesSinceCreatedAt)) {
            it.int("count")
        }
    }
}
