package no.nav.mulighetsrommet.tiltak.okonomi.oebs

import kotlinx.serialization.Serializable

@Serializable
data class OebsBestilling(
    val id: String,
    val status: Status,
) {

    enum class Status {
        NY,
        BEHANDLET,
        FEILET,
    }
}
