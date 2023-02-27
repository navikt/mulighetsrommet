package no.nav.mulighetsrommet.arena.adapter.models.db

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class Tiltaksgjennomforing(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltaksgjennomforingId: Int,
    val sakId: Int,
    val tiltakskode: String,
    val arrangorId: Int?,
    val navn: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val fraDato: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val tilDato: LocalDateTime? = null,
    val apentForInnsok: Boolean = true,
    val antallPlasser: Int? = null,
    val status: String
)
