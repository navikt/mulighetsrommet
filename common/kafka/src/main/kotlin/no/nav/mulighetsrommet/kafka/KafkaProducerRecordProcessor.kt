package no.nav.mulighetsrommet.kafka

import kotlinx.coroutines.*
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.common.kafka.util.KafkaUtils
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.function.Consumer

interface ProducerRecordRepository {
    fun getRecords(): List<ProducerRecordDbo>
    fun deleteRecords(ids: List<Long>)
}

class KafkaProducerRecordProcessor(
    private val repository: ProducerRecordRepository,
    private val producerClient: KafkaProducerClient<ByteArray, ByteArray>,
) {
    private val pollTimeout: Long = 3000

    private val log: Logger = LoggerFactory.getLogger(this.javaClass)
    private var job: Job? = null

    fun start() {
        if (job != null) return

        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                recordHandler()
            }
        }.apply {
            invokeOnCompletion {
                log.info("Shutting down Kafka producer")
                producerClient.close()
            }
        }
    }

    fun close() {
        job?.cancel()
        job = null
    }

    private suspend fun recordHandler() {
        try {
            val records = repository.getRecords()

            if (records.isNotEmpty()) {
                publishRecords(records)
            } else {
                delay(pollTimeout)
            }
        } catch (e: Exception) {
            log.error("Failed to process kafka producer records", e)
            delay(pollTimeout)
        }
    }

    private fun publishRecords(records: List<ProducerRecordDbo>) {
        val idsToDelete = Collections.synchronizedList(mutableListOf<Long>())

        val latch = CountDownLatch(records.size)

        records.forEach(
            Consumer { record ->
                producerClient.send(record.toProducerRecord()) { _: RecordMetadata?, exception: java.lang.Exception? ->
                    latch.countDown()
                    if (exception != null) {
                        log.warn("Failed to send record to topic ${record.topic}", exception)
                    } else {
                        idsToDelete.add(record.id)
                    }
                }
            },
        )

        producerClient.producer.flush()
        latch.await()

        repository.deleteRecords(idsToDelete)
    }
}

fun ProducerRecordDbo.toProducerRecord(): ProducerRecord<ByteArray, ByteArray> {
    val headers = KafkaUtils.jsonToHeaders(headersJson)

    return ProducerRecord(
        topic,
        null,
        null,
        key,
        value,
        headers,
    )
}
