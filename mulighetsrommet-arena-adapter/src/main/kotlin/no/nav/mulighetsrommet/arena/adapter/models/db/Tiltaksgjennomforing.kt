package no.nav.mulighetsrommet.arena.adapter.models.db

import java.time.LocalDateTime
import java.util.*

data class Tiltaksgjennomforing(
    val id: UUID,
    val sanityId: UUID?,
    val tiltaksgjennomforingId: Int,
    val sakId: Int,
    val tiltakskode: String,
    val arrangorId: Int,
    val navn: String,
    val fraDato: LocalDateTime,
    val tilDato: LocalDateTime?,
    val apentForPamelding: Boolean,
    val antallPlasser: Int?,
    val status: String,
    val avtaleId: Int?,
    val deltidsprosent: Double,
)
