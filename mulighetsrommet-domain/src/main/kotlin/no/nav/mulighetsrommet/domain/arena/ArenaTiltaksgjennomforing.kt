package no.nav.mulighetsrommet.domain.arena

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.DateSerializer
import java.time.LocalDateTime

@Serializable
data class ArenaTiltaksgjennomforing(
    val id: Int,
    val lokalnavn: String?,
    val tiltakskode: String,
    val arrangorId: Int?,
    val sakId: Int,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime? = null
)
