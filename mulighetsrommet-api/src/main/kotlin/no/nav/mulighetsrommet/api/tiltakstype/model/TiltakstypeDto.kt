package no.nav.mulighetsrommet.api.tiltakstype.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.TiltakstypeStatus
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class TiltakstypeDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val innsatsgrupper: Set<Innsatsgruppe>,
    val arenaKode: String,
    val tiltakskode: Tiltakskode?,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val status: TiltakstypeStatus,
    @Serializable(with = UUIDSerializer::class)
    val sanityId: UUID?,
)
