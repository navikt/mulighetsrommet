package no.nav.mulighetsrommet.arena.adapter.kafka

data class ConsumerGroup(
    val consumers: List<TopicConsumer<out Any>>
)
