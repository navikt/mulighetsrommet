package no.nav.tiltak.historikk.model

import no.nav.mulighetsrommet.model.DeltakerStatusAarsakType
import no.nav.mulighetsrommet.model.DeltakerStatusType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class KometDeltaker(
    val id: UUID,
    val gjennomforingId: UUID,
    val personIdent: String,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val statusType: DeltakerStatusType,
    val statusOpprettetTidspunkt: LocalDateTime,
    val statusAarsak: DeltakerStatusAarsakType?,
    val registrertTidspunkt: LocalDateTime,
    val endretTidspunkt: LocalDateTime,
    val dagerPerUke: Float?,
    val prosentStilling: Float?,
)
