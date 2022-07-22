package no.nav.mulighetsrommet.arena.adapter.services

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.arena.adapter.createDatabaseConfigWithRandomSchema
import no.nav.mulighetsrommet.arena.adapter.kafka.TopicConsumer
import no.nav.mulighetsrommet.arena.adapter.test.extensions.DatabaseListener
import org.assertj.db.api.Assertions.assertThat
import org.assertj.db.type.Table

class TopicServiceTest : ShouldSpec({

    testOrder = TestCaseOrder.Sequential

    val listener = DatabaseListener(createDatabaseConfigWithRandomSchema())
    register(listener)

    lateinit var topicService: TopicService
    lateinit var table: Table

    beforeSpec {
        topicService = TopicService(listener.db)
        table = Table(listener.db.dataSource, "topics")
    }


    should("create a list of topics if not exists") {

        val topicConsumer1 = mockk<TopicConsumer<Any>>()
        val topicConsumer2 = mockk<TopicConsumer<Any>>()

        every { topicConsumer1.topic } returns "topic1"
        every { topicConsumer1.key } returns "topic1"
        every { topicConsumer2.topic } returns "topic2"
        every { topicConsumer2.key } returns "topic2"

        topicService.upsertConsumerTopics(listOf(topicConsumer1, topicConsumer2))

        assertThat(table).row(0)
            .value().isEqualTo(1)
            .value().isEqualTo(topicConsumer1.key)
            .value().isEqualTo(topicConsumer1.topic)
            .value().isEqualTo(TopicType.CONSUMER.name)
            .value().isEqualTo(false)

        assertThat(table).row(1)
            .value().isEqualTo(2)
            .value().isEqualTo(topicConsumer2.key)
            .value().isEqualTo(topicConsumer2.topic)
            .value().isEqualTo(TopicType.CONSUMER.name)
            .value().isEqualTo(false)

        assertThat(table).row().value(2)
    }

    should("update existing topics") {

        val topicConsumer1 = mockk<TopicConsumer<Any>>()
        val topicConsumer2 = mockk<TopicConsumer<Any>>()

        every { topicConsumer1.topic } returns "topicChanged1"
        every { topicConsumer1.key } returns "topic1"
        every { topicConsumer2.topic } returns "topicChanged2"
        every { topicConsumer2.key } returns "topic2"


        topicService.upsertConsumerTopics(listOf(topicConsumer1, topicConsumer2))

        assertThat(table).row(0)
            .value().isEqualTo(1)
            .value().isEqualTo(topicConsumer1.key)
            .value().isEqualTo("topic1")
            .value().isEqualTo(TopicType.CONSUMER.name)
            .value().isEqualTo(false)

        assertThat(table).row(1)
            .value().isEqualTo(2)
            .value().isEqualTo(topicConsumer2.key)
            .value().isEqualTo("topic2")
            .value().isEqualTo(TopicType.CONSUMER.name)
            .value().isEqualTo(false)

        assertThat(table).row().value(2)
    }

    should("return a list of topics") {
        val topics = topicService.getTopics()

        topics shouldHaveSize 2
        topics[0].key shouldBe "topic1"
        topics[0].topic shouldBe "topicChanged1"
        topics[1].key shouldBe "topic2"
        topics[1].topic shouldBe "topicChanged2"
    }

    should("update running state for topics") {
        val updatedTopic = mockk<TopicService.Topic>()

        every { updatedTopic.id } returns 1
        every { updatedTopic.running } returns true

        val updatedTopics = topicService.updateRunningStateByTopics(listOf(updatedTopic))

        updatedTopics shouldHaveSize 1
        updatedTopics[0].running shouldBe true
    }

    should("only return the changed topic(s)") {
        val updatedTopic = mockk<TopicService.Topic>()
        val existingTopic = mockk<TopicService.Topic>()

        every { updatedTopic.id } returns 1
        every { updatedTopic.running } returns true
        every { existingTopic.id } returns 2
        every { existingTopic.running } returns true

        val updatedTopics = topicService.updateRunningStateByTopics(listOf(updatedTopic, existingTopic))

        updatedTopics shouldHaveSize 1
        updatedTopics[0].running shouldBe true
    }

})
