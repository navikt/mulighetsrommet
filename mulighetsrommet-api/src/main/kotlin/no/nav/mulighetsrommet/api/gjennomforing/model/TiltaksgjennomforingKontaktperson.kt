package no.nav.mulighetsrommet.api.gjennomforing.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.NavIdent

@Serializable
data class TiltaksgjennomforingKontaktperson(
    val navIdent: NavIdent,
    val navn: String,
    val epost: String,
    val mobilnummer: String? = null,
    val navEnheter: List<String>,
    val hovedenhet: String,
    val beskrivelse: String?,
)
