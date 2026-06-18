package no.nav.mulighetsrommet.api.utbetaling.db

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DeltakerDbo(
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
    data class Deltakelsesmengde(
        val gyldigFra: LocalDate,
        val deltakelsesprosent: Double,
        val opprettetTidspunkt: LocalDateTime,
    )

    @Serializable
    data class NavVeileder(
        val navIdent: NavIdent,
        val enhetsnummer: NavEnhetNummer?,
    )
}
