package no.nav.mulighetsrommet.api.clients.ssb

import kotlinx.serialization.Serializable

@Serializable
data class SsbNusData(
    val validFrom: String,
    val classificationItems: List<ClassificationItem>,
    val _links: Link,
)

@Serializable
data class ClassificationItem(
    val code: String,
    val parentCode: String,
    val level: String,
    val name: String,
    val shortName: String? = null,
)

@Serializable
data class Link(
    val self: Self,
) {
    @Serializable
    class Self(
        val href: String,
    )
}
