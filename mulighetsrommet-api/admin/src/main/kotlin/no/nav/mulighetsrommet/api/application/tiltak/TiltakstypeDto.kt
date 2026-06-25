package no.nav.mulighetsrommet.api.application.tiltak

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold.RedaksjoneltInnholdLenke
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeFeature
import no.nav.mulighetsrommet.model.DeltakerRegistreringInnholdDto
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.TiltakstypeEgenskap
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class TiltakstypeDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val tiltakskode: Tiltakskode,
    val gruppe: String?,
    @Serializable(with = UUIDSerializer::class)
    val sanityId: UUID?,
    val features: Set<TiltakstypeFeature>,
    val egenskaper: Set<TiltakstypeEgenskap>,
    val veilederinfo: TiltakstypeVeilderinfo,
    val deltakerinfo: DeltakerRegistreringInnholdDto?,
)

@Serializable
data class TiltakstypeVeilderinfo(
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
    val faglenker: List<RedaksjoneltInnholdLenke>,
    val kanKombineresMed: List<TiltakstypeKombinasjon>,
)

@Serializable
data class TiltakstypeKombinasjon(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
)
