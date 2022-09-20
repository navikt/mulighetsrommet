package no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import no.nav.common.kafka.consumer.feilhandtering.StoredConsumerRecord
import no.nav.mulighetsrommet.arena.adapter.kafka.KafkaConsumerRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import org.assertj.db.api.Assertions
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
        table = Table(listener.db.getDatasource(), "failed_events")
    }

    test("should store records") {
        val record = StoredConsumerRecord("topic", 0, 0L, "key".toByteArray(), "value".toByteArray(), "{}", LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
        kafkaConsumerRepository.storeRecord(record)
        Assertions.assertThat(table).hasNumberOfRows(1)
    }
})
