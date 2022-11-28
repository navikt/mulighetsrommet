package no.nav.mulighetsrommet.domain.adapter

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.serializers.DateSerializer
import java.time.LocalDateTime

@Serializable
data class AdapterTiltaksgjennomforing(
    val tiltaksgjennomforingId: Int,
    val sakId: Int,
    val tiltakskode: String,
    val arrangorId: Int?,
    val navn: String?,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime? = null,
    val apentForInnsok: Boolean = true,
    val antallPlasser: Int? = null,
)
