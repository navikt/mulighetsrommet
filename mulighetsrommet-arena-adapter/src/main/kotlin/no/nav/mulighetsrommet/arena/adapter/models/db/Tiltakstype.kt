package no.nav.mulighetsrommet.arena.adapter.models.db

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.DateSerializer
import java.time.LocalDateTime
import java.util.*

@Serializable
data class Tiltakstype(
    val navn: String,
    val innsatsgruppe: Int,
    val tiltakskode: String,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime? = null
)
