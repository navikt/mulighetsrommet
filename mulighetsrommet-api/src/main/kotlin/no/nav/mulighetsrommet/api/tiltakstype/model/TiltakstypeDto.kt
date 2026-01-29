package no.nav.mulighetsrommet.api.tiltakstype.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.TiltakstypeEgenskap
import no.nav.mulighetsrommet.model.TiltakstypeStatus
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

@Serializable
data class TiltakstypeDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val tiltakskode: Tiltakskode,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val status: TiltakstypeStatus,
    @Serializable(with = UUIDSerializer::class)
    val sanityId: UUID?,
    val features: Set<TiltakstypeFeature>,
    val egenskaper: Set<TiltakstypeEgenskap>,
)

enum class TiltakstypeFeature {
    /**
     * Vises i Tiltaksadministrasjon
     */
    VISES_I_TILTAKSADMINISTRASJON,

    /**
     * Kan opprettes i Tiltaksadministrasjon
     */
    KAN_OPPRETTE_AVTALE,

    /**
     * Administreres i Tiltaksadministrasjon og deles med Arena
     */
    MIGRERT,
}
