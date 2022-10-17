package no.nav.mulighetsrommet.domain.models

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.DateSerializer
import java.time.LocalDateTime
import java.util.*

enum class Deltakerstatus {
    IKKE_AKTUELL,
    VENTER,
    DELTAR,
    AVSLUTTET
}

@Serializable
data class Deltaker(
    val id: Int? = null,
    val tiltaksgjennomforingId: Int,
    val personId: Int,
    val status: Deltakerstatus
)

@Serializable
data class HistorikkForDeltakerDTO(
    val id: String,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime? = null,
    val status: Deltakerstatus,
    val tiltaksnavn: String,
    val tiltaksnummer: String,
    val tiltakstype: String,
    val arrangor: String?
)

data class HistorikkForDeltaker(
    val id: String,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime? = null,
    val status: Deltakerstatus,
    val tiltaksnavn: String,
    val tiltaksnummer: String,
    val tiltakstype: String,
    val arrangorId: Int
)
