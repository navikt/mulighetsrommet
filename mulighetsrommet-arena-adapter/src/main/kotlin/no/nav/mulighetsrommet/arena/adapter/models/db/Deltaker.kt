package no.nav.mulighetsrommet.arena.adapter.models.db

import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import java.time.LocalDateTime
import java.util.*

data class Deltaker(
    val id: UUID,
    val tiltaksdeltakerId: Int,
    val tiltaksgjennomforingId: Int,
    val personId: Int,
    val status: Deltakerstatus,
    val fraDato: LocalDateTime? = null,
    val tilDato: LocalDateTime? = null,
)
