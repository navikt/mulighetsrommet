package no.nav.mulighetsrommet.api.clients.ssb

import kotlinx.serialization.Serializable

@Serializable
data class SsbNusData(
    val validFrom: String,
    val classificationItems: List<ClassificationItem>,
)

@Serializable
data class ClassificationItem(
    val code: String,
    val parentCode: String,
    val level: String,
    val name: String,
    val shortName: String? = null,
)
