package no.nav.mulighetsrommet.api.domain.deltaker

import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusAarsakType
import no.nav.mulighetsrommet.model.DeltakerStatusType
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

data class Deltaker private constructor(
    val id: UUID,
    val gjennomforingId: UUID,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val registrertTidspunkt: Instant,
    val endretTidspunkt: Instant,
    val status: DeltakerStatus,
    val deltakelsesmengder: List<Deltakelsesmengde>,
    val innholdAnnet: String?,
    val navVeileder: NavVeileder?,
) {
    fun erFeilregistrert(): Boolean = status.type == DeltakerStatusType.FEILREGISTRERT

    fun registrerStatus(
        type: DeltakerStatusType,
        aarsak: DeltakerStatusAarsakType? = null,
        endretTidspunkt: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS),
    ): Deltaker {
        return copy(
            endretTidspunkt = endretTidspunkt,
            status = DeltakerStatus(type, aarsak, endretTidspunkt),
        )
    }

    companion object {
        /**
         * Postgres lagrer kun med mikrosekunders presisjon (timestamptz). Tidspunktene avkortes derfor
         * her slik at en [Deltaker] alltid har en presisjon som også overlever lagring.
         * Dette er bl.a. avgjørende for duplikatsjekk ifm. oppdatering av enkeltplass-gjennomføringer.
         */
        fun opprett(
            id: UUID,
            gjennomforingId: UUID,
            startDato: LocalDate?,
            sluttDato: LocalDate?,
            registrertTidspunkt: Instant,
            endretTidspunkt: Instant,
            status: DeltakerStatus,
            deltakelsesmengder: List<Deltakelsesmengde>,
            innholdAnnet: String?,
            navVeileder: NavVeileder?,
        ) = Deltaker(
            id = id,
            gjennomforingId = gjennomforingId,
            startDato = startDato,
            sluttDato = sluttDato,
            registrertTidspunkt = registrertTidspunkt.truncatedTo(ChronoUnit.MICROS),
            endretTidspunkt = endretTidspunkt.truncatedTo(ChronoUnit.MICROS),
            status = status.copy(opprettetTidspunkt = status.opprettetTidspunkt.truncatedTo(ChronoUnit.MICROS)),
            deltakelsesmengder = deltakelsesmengder,
            innholdAnnet = innholdAnnet,
            navVeileder = navVeileder,
        )
    }
}
