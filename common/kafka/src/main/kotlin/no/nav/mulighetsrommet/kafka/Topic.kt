package no.nav.mulighetsrommet.kafka

data class Topic(
    val id: String,
    val topic: String,
    val type: TopicType,
    val running: Boolean,
)
