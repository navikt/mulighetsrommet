package no.nav.tiltak.historikk.model

import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.Instant
import java.util.UUID

data class Tiltakstype(
    val tiltakstypeId: UUID,
    val navn: String,
    val tiltakskode: Tiltakskode?,
    val arenaTiltakskode: String?,
    val opprettetTidspunkt: Instant?,
    val oppdatertTidspunkt: Instant?,
)
