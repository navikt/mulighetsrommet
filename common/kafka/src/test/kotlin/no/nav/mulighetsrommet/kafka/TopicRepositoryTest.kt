package no.nav.mulighetsrommet.kafka

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener

class TopicRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(testDatabaseConfig))

    val topic0 = Topic(id = "0", topic = "topic-0", type = TopicType.CONSUMER, running = false)
    val topic1 = Topic(id = "1", topic = "topic-1", type = TopicType.CONSUMER, running = false)
    val topic2 = Topic(id = "2", topic = "topic-2", type = TopicType.CONSUMER, running = false)
    val topics = listOf(topic0, topic1, topic2)

    test("set and get topics") {
        val repository = TopicRepository(database.db)

        repository.setAll(topics)
        repository.getAll() shouldBe topics

        val updatedTopics = listOf(
            topic0.copy(running = true),
            Topic(id = "new-id", topic = "new-topic", type = TopicType.CONSUMER, running = false),
        )

        repository.setAll(updatedTopics)
        repository.getAll() shouldBe updatedTopics
    }

    test("update running state") {
        val repository = TopicRepository(database.db)
        repository.setAll(topics)

        val updatedTopic0 = topic0.copy(running = true)
        repository.updateRunning(listOf(updatedTopic0))

        repository.getAll() shouldBe listOf(updatedTopic0, topic1, topic2)
    }
})
