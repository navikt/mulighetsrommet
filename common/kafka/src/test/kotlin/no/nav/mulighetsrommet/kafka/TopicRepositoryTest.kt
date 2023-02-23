package no.nav.mulighetsrommet.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.clearAllMocks
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema

class TopicRepositoryTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createArenaAdapterDatabaseTestSchema()))

    lateinit var topicRepository: TopicRepository

    beforeSpec {
        topicRepository = TopicRepository(database.db)
    }

    beforeEach {
        clearAllMocks()
    }

    test("should create new topics if none exists") {
        val topics = (0..2).map {
            Topic(id = "key$it", topic = "topic$it", type = TopicType.CONSUMER, running = false)
        }

        topicRepository.upsertTopics(topics)

        database.assertThat("topics")
            .row()
            .value("id").isEqualTo("key0")
            .value("topic").isEqualTo("topic0")
            .value("running").isEqualTo(false)
            .row()
            .value("id").isEqualTo("key1")
            .value("topic").isEqualTo("topic1")
            .value("running").isEqualTo(false)
            .row()
            .value("id").isEqualTo("key2")
            .value("topic").isEqualTo("topic2")
            .value("running").isEqualTo(false)
    }

    test("should update existing topics") {
        val topic = Topic(id = "key0", topic = "topic-changed", type = TopicType.CONSUMER, running = true)

        topicRepository.upsertTopics(listOf(topic))

        database.assertThat("topics")
            .row(0)
            .value("id").isEqualTo("key0")
            .value("topic").isEqualTo("topic-changed")
            .value("running").isEqualTo(true)
    }

    test("should get all topics") {
        topicRepository.selectAll() shouldHaveSize 3
    }
})
