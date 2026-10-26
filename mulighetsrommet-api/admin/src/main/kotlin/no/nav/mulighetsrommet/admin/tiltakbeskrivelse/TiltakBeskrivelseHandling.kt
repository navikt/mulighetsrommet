package no.nav.mulighetsrommet.admin.tiltakbeskrivelse

import kotlinx.serialization.Serializable

@Serializable
enum class TiltakBeskrivelseHandling {
    PUBLISER,
    REDIGER,
    FORHANDSVIS_I_MODIA,
}
