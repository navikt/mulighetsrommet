package no.nav.mulighetsrommet.api.tiltakstype.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class TiltakstypeVeilederinfoRequest(
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
    val faglenker: List<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        >,
    val kanKombineresMed: List<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        >,
)
