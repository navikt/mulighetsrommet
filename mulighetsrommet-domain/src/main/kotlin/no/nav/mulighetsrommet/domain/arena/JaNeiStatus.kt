package no.nav.mulighetsrommet.domain.arena

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class JaNeiStatus {

    @SerialName("J")
    Ja,

    @SerialName("N")
    Nei,

}
