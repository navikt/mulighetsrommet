package no.nav.mulighetsrommet.arena

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class JaNeiStatus {

    @SerialName("J")
    Ja,

    @SerialName("N")
    Nei,
}
