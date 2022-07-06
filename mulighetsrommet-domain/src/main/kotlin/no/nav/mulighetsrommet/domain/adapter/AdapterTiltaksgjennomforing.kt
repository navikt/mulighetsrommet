package no.nav.mulighetsrommet.domain.adapter

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.DateSerializer
import no.nav.mulighetsrommet.domain.Deltakerstatus
import java.time.LocalDateTime

@Serializable
data class AdapterTiltaksgjennomforing (
    val id: Int,
    val navn: String,
    val tiltakskode: String,
    val arrangorId: Int?,
    val tiltaksnummer: Int,
    val sakId: Int,
    @Serializable(with = DateSerializer::class)
    val fraDato: LocalDateTime? = null,
    @Serializable(with = DateSerializer::class)
    val tilDato: LocalDateTime? = null,
)
