package no.nav.mulighetsrommet.api.veilederflate

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold.RedaksjoneltInnholdLenke
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

interface VeilederflateTiltakstypeQueryHandler {
    fun getVeilederinfo(id: UUID): VeilederflateTiltakstypeVeilederinfo?
}

@Serializable
data class VeilederflateTiltakstypeVeilederinfo(
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
    val faglenker: List<RedaksjoneltInnholdLenke>,
    val kanKombineresMed: List<VeilederflateTiltakstypeKombinasjon>,
)

@Serializable
data class VeilederflateTiltakstypeKombinasjon(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
)
