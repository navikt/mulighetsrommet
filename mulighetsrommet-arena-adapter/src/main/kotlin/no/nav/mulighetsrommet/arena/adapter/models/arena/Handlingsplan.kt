package no.nav.mulighetsrommet.arena.adapter.models.arena

import kotlinx.serialization.SerialName

@kotlinx.serialization.Serializable
enum class Handlingsplan {

    @SerialName("AKT")
    AKT,

    @SerialName("LAG")
    LAG,

    @SerialName("SOK")
    SOK,

    @SerialName("TIL")
    TIL
}
