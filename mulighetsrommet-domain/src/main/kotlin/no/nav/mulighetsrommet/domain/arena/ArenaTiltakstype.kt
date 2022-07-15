package no.nav.mulighetsrommet.domain.arena

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.DateSerializer
import java.time.LocalDateTime

@Serializable
data class ArenaTiltakstype(
    val tiltakskode: String,
    val tiltaksnavn: String,
    val arrangorId: Int?,
    val sakId: Int,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime? = null,
)
