package no.nav.mulighetsrommet.maam.dto

import kotlinx.serialization.Serializable

@Serializable
data class KafkaTopicConsumerDto(
    val id: String,
    val topic: String,
    val running: Boolean,
)
