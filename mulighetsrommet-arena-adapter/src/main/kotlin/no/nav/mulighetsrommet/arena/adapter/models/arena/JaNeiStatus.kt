package no.nav.mulighetsrommet.arena.adapter.models.arena

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class JaNeiStatus {

    @SerialName("J")
    Ja,

    @SerialName("N")
    Nei,
}
