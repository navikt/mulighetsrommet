package no.nav.mulighetsrommet.arena.adapter.kafka

data class ConsumerGroup<T : TopicConsumer>(
    val consumers: List<T>
)
