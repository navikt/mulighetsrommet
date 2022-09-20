package no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe
import no.nav.common.kafka.consumer.feilhandtering.StoredConsumerRecord
import no.nav.mulighetsrommet.arena.adapter.kafka.KafkaConsumerRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import org.assertj.db.api.Assertions
import org.assertj.db.type.Changes
import org.assertj.db.type.Table
import java.time.LocalDateTime
import java.time.ZoneOffset

class KafkaConsumerRepositoryTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val listener =
        FlywayDatabaseListener(createArenaAdapterDatabaseTestSchema())
    register(listener)

    lateinit var kafkaConsumerRepository: KafkaConsumerRepository
    lateinit var table: Table

    beforeSpec {
        kafkaConsumerRepository = KafkaConsumerRepository(listener.db)
    }

    beforeEach {
        table = Table(listener.db.getDatasource(), "failed_events")
    }

    test("should store records") {
        val records = (0..2).map {
            StoredConsumerRecord("topic$it", 0, 0L, "key$it".toByteArray(), "value$it".toByteArray(), "{}", LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
        }
        records.forEach { kafkaConsumerRepository.storeRecord(it) }
        Assertions.assertThat(table).hasNumberOfRows(3)
    }

    test("should delete records") {
        kafkaConsumerRepository.deleteRecords(mutableListOf(1))
        Assertions.assertThat(table).hasNumberOfRows(2)
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
        val changes = Changes(listener.db.getDatasource()).setTables(table)
        Assertions.assertThat(table).column("retries").value().isEqualTo(0)
        changes.setStartPointNow()
        kafkaConsumerRepository.incrementRetries(2)
        changes.setEndPointNow()
        Assertions.assertThat(changes).hasNumberOfChanges(1)
        Assertions.assertThat(changes).changeOnTableWithPks("failed_events", 2)
            .isModification
            .hasNumberOfModifiedColumns(4)
    }

    test("should get topic partitions") {
        kafkaConsumerRepository.storeRecord(
            StoredConsumerRecord("topic1", 1, 0L, "key1".toByteArray(), "value1".toByteArray(), "{}", LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
        )
        val result = kafkaConsumerRepository.getTopicPartitions(mutableListOf("topic1"))
        result.size shouldBe 2
    }

})
