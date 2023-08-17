package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class JaNeiStatus {

    @SerialName("J")
    Ja,

    @SerialName("N")
    Nei,
}
