package no.nav.mulighetsrommet.arena.adapter.models.db

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import java.time.LocalDateTime
import java.util.*

data class Tiltaksgjennomforing(
    val id: UUID,
    val tiltaksgjennomforingId: Int,
    val sakId: Int,
    val tiltakskode: String,
    val arrangorId: Int?,
    val navn: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val fraDato: LocalDateTime,
    val tilDato: LocalDateTime? = null,
    val apentForInnsok: Boolean = true,
    val antallPlasser: Int? = null,
    val status: String
)
