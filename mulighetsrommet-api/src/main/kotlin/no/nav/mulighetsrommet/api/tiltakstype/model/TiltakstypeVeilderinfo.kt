package no.nav.mulighetsrommet.api.tiltakstype.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Faneinnhold

@Serializable
data class TiltakstypeVeilderinfo(
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
    val faglenker: List<RedaksjoneltInnholdLenke>,
    val kanKombineresMed: List<String>,
)
