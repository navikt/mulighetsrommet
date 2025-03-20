package no.nav.mulighetsrommet.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.common.kafka.producer.feilhandtering.StoredProducerRecord
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener

class KafkaProducerRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(testDatabaseConfig))

    lateinit var kafkaProducerRepository: KafkaProducerRepositoryImpl

    beforeSpec {
        kafkaProducerRepository = KafkaProducerRepositoryImpl(database.db)
    }

    test("should store records") {
        val records = (0..2).map {
            StoredProducerRecord(
                "topic$it",
                "key$it".toByteArray(),
                "value$it".toByteArray(),
                "{}",
            )
        }

        records.forEach { kafkaProducerRepository.storeRecord(it) }

        database.assertTable("kafka_producer_record").hasNumberOfRows(3)
    }

    test("should delete records") {
        kafkaProducerRepository.deleteRecords(listOf(1, 2))
        database.assertTable("kafka_producer_record").hasNumberOfRows(1)
    }

    test("should get all records") {
        val result = kafkaProducerRepository.getRecords(10)
        result.size shouldBe 1
        result[0].id shouldBe 3
    }

    test("should get records by topics") {
        kafkaProducerRepository.storeRecord(
            StoredProducerRecord(
                "topic1",
                "key1".toByteArray(),
                "value1".toByteArray(),
                "{}",
            ),
        )

        val result = kafkaProducerRepository.getRecords(10, listOf("topic1"))
        result.size shouldBe 1
    }
})
