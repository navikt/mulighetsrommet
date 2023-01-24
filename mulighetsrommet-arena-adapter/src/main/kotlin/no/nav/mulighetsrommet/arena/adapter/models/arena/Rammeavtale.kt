package no.nav.mulighetsrommet.arena.adapter.models.arena

import kotlinx.serialization.SerialName

@kotlinx.serialization.Serializable
enum class Rammeavtale {
    @SerialName("KAN")
    KAN,

    @SerialName("SKAL")
    SKAL,

    @SerialName("IKKE")
    IKKE
}
