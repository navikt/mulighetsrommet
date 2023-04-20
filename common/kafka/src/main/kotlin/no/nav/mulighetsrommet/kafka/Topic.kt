package no.nav.mulighetsrommet.kafka

import kotlinx.serialization.Serializable

@Serializable
data class Topic(
    val id: String,
    val topic: String,
    val type: TopicType,
    val running: Boolean,
)
