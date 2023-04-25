package no.nav.mulighetsrommet.arena.adapter.models.arena

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Administrasjonskode {
    @SerialName("IND")
    IND,

    @SerialName("AMO")
    AMO,

    @SerialName("INST")
    INST,
}
