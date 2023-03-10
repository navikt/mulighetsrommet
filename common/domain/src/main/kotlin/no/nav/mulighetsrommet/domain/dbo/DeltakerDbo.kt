package no.nav.mulighetsrommet.domain.dbo

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class DeltakerDbo(
    val id: UUID,
    val tiltaksgjennomforingId: UUID,
    val norskIdent: String,
    val status: Deltakerstatus,
    val startDato: LocalDate?,
    val sluttDato: LocalDate?,
    val registrertDato: LocalDateTime,
)
