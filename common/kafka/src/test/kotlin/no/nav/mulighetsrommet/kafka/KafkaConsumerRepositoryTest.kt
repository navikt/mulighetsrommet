package no.nav.mulighetsrommet.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.common.kafka.consumer.feilhandtering.StoredConsumerRecord
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createRandomDatabaseConfig
import java.time.LocalDateTime
import java.time.ZoneOffset

class KafkaConsumerRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createRandomDatabaseConfig()))

    lateinit var kafkaConsumerRepository: KafkaConsumerRepositoryImpl

    beforeSpec {
        kafkaConsumerRepository = KafkaConsumerRepositoryImpl(database.db)
    }

    test("should store records") {
        val records = (0..2).map {
            StoredConsumerRecord(
                "topic$it",
                0,
                0L,
                "key$it".toByteArray(),
                "value$it".toByteArray(),
                "{}",
                LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
            )
        }

        records.forEach { kafkaConsumerRepository.storeRecord(it) }

        database.assertThat("failed_events").hasNumberOfRows(3)
    }

    test("should delete records") {
        kafkaConsumerRepository.deleteRecords(mutableListOf(1))
        database.assertThat("failed_events").hasNumberOfRows(2)
    }

    test("should retrieve correct key") {
        val result = kafkaConsumerRepository.hasRecordWithKey("topic1", 0, "key1".toByteArray())
        result shouldBe true
    }

    test("should get all records") {
        val result = kafkaConsumerRepository.getRecords("topic1", 0, 10)
        result.size shouldBe 1
        result[0].id shouldBe 2
    }

    test("should increment retries") {
        database.assertThat("failed_events").row().value("retries").isEqualTo(0)

        kafkaConsumerRepository.incrementRetries(2)

        database.assertThat("failed_events").row().value("retries").isEqualTo(1)
    }

    test("should get topic partitions") {
        kafkaConsumerRepository.storeRecord(
            StoredConsumerRecord(
                "topic1",
                1,
                0L,
                "key1".toByteArray(),
                "value1".toByteArray(),
                "{}",
                LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
            ),
        )

        val result = kafkaConsumerRepository.getTopicPartitions(mutableListOf("topic1"))

        result.size shouldBe 2
    }
})
