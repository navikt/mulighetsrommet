package no.nav.mulighetsrommet.domain.adapter

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.DateSerializer
import java.time.LocalDateTime

@Serializable
data class AdapterTiltak (
    val navn: String,
    val innsatsgruppe: Int,
    val tiltakskode: String,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime? = null
)
