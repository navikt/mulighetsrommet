package no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.ints.shouldBeExactly
import io.mockk.*
import no.nav.mulighetsrommet.arena.adapter.ConsumerConfig
import no.nav.mulighetsrommet.arena.adapter.kafka.TopicConsumer
import no.nav.mulighetsrommet.arena.adapter.repositories.Topic
import no.nav.mulighetsrommet.arena.adapter.repositories.TopicRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import org.assertj.db.api.Assertions
import org.assertj.db.type.Table

class TopicRepositoryTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val listener =
        FlywayDatabaseListener(createArenaAdapterDatabaseTestSchema())
    register(listener)

    lateinit var topicRepository: TopicRepository
    lateinit var table: Table

    beforeSpec {
        topicRepository = TopicRepository(listener.db)
    }

    beforeEach() {
        table = Table(listener.db.getDatasource(), "topics")
        clearAllMocks()
    }

    test("should create new topics if non exists") {
        val topicConsumers = (0..2).map { mockk<TopicConsumer<Any>>(relaxed = true) }
        topicConsumers.forEachIndexed { index, it ->
            every { it.consumerConfig } returns ConsumerConfig(
                "key$index",
                "topic$index"
            )
        }
        topicRepository.upsertTopics(topicConsumers)
        Assertions.assertThat(table).hasNumberOfRows(3)
        Assertions.assertThat(table).column("key")
            .value().isEqualTo("key0")
            .value().isEqualTo("key1")
            .value().isEqualTo("key2")
    }

    test("should update existing topics") {
        val topicConsumer = mockk<TopicConsumer<Any>>(relaxed = true)
        every { topicConsumer.consumerConfig } returns ConsumerConfig(
            "key0",
            "topic-changed"
        )
        topicRepository.upsertTopics(listOf(topicConsumer))
        Assertions.assertThat(table).row(0)
            .value().isEqualTo(1)
            .value().isEqualTo("key0")
            .value().isEqualTo("topic-changed")
    }

    test("should get all topics") {
        val topics = topicRepository.selectAll()
        topics.size shouldBeExactly 3
    }

    test("should update running state") {
        val topics = listOf(
            mockk<Topic>(),
            mockk(),
        )

        topics.forEachIndexed { index, it ->
            every { it.id } returns index+1
            every { it.running } returns true
        }

        topicRepository.updateRunning(topics)

        val r = topicRepository.selectAll()

        r.forEach { println(it) }

        Assertions.assertThat(table).column("running")
            .value().isEqualTo(true)
            .value().isEqualTo(true)
            .value().isEqualTo(false)
    }
})
