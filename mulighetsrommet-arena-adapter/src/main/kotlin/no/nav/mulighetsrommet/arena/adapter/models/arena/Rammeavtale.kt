package no.nav.mulighetsrommet.arena.adapter.models.arena

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Rammeavtale {
    @SerialName("KAN")
    KAN,

    @SerialName("SKAL")
    SKAL,

    @SerialName("IKKE")
    IKKE
}
