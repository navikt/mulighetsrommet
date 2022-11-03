package no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.clearAllMocks
import no.nav.mulighetsrommet.arena.adapter.repositories.Topic
import no.nav.mulighetsrommet.arena.adapter.repositories.TopicRepository
import no.nav.mulighetsrommet.arena.adapter.repositories.TopicType
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseListener
import no.nav.mulighetsrommet.database.kotest.extensions.createArenaAdapterDatabaseTestSchema
import org.assertj.db.api.Assertions.assertThat
import org.assertj.db.type.Table

class TopicRepositoryTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val listener = extension(FlywayDatabaseListener(createArenaAdapterDatabaseTestSchema()))

    lateinit var topicRepository: TopicRepository
    lateinit var table: Table

    beforeSpec {
        topicRepository = TopicRepository(listener.db)
    }

    beforeEach {
        table = Table(listener.db.getDatasource(), "topics")
        clearAllMocks()
    }

    test("should create new topics if non exists") {
        val topics = (0..2).map {
            Topic(id = "key$it", topic = "topic$it", type = TopicType.CONSUMER, running = false)
        }

        topicRepository.upsertTopics(topics)

        assertThat(table)
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

        assertThat(table)
            .row(0)
            .value("id").isEqualTo("key0")
            .value("topic").isEqualTo("topic-changed")
            .value("running").isEqualTo(true)
    }

    test("should get all topics") {
        topicRepository.selectAll() shouldHaveSize 3
    }
})
