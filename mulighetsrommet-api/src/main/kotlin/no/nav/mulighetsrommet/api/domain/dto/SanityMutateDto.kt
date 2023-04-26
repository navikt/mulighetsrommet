package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class Mutations(
    val mutations: List<Mutation>,
)

@Serializable
data class Mutation(
    val createOrReplace: SanityEnhet,
)

@Serializable
data class SanityEnhet(
    val _id: String,
    val _type: String = "enhet",
    val navn: String,
    val nummer: EnhetSlug,
    val type: String,
    val status: String,
    val fylke: FylkeRef?,
)

@Serializable
data class EnhetSlug(
    val _type: String = "slug",
    val current: String,
)

@Serializable
data class FylkeRef(
    val _type: String = "reference",
    val _ref: String,
    val _key: String,
)
