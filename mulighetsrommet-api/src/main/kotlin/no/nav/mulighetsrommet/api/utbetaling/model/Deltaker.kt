package no.nav.mulighetsrommet.api.utbetaling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Deltaker(
    val id: UUID,
    val gjennomforingId: UUID,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val registrertTidspunkt: LocalDateTime,
    val endretTidspunkt: LocalDateTime,
    val status: DeltakerStatus,
    val deltakelsesmengder: List<Deltakelsesmengde>,
    val innholdAnnet: String?,
    val navVeileder: NavVeileder?,
) {
    fun erFeilregistrert(): Boolean = status.type == DeltakerStatusType.FEILREGISTRERT
}

@Serializable
data class Deltakelsesmengde(
    @Serializable(with = LocalDateSerializer::class)
    val gyldigFra: LocalDate,
    val deltakelsesprosent: Double,
)

@Serializable
data class NavVeileder(
    val navIdent: NavIdent,
    val enhetsnummer: NavEnhetNummer?,
)
