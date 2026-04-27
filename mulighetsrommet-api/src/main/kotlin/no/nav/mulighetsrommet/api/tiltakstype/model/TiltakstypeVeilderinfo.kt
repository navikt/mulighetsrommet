package no.nav.mulighetsrommet.api.tiltakstype.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

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
