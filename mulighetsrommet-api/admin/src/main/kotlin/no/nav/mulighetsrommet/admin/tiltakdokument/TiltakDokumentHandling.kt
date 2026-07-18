package no.nav.mulighetsrommet.admin.tiltakdokument

import kotlinx.serialization.Serializable

@Serializable
enum class TiltakDokumentHandling {
    PUBLISER,
    REDIGER,
    FORHANDSVIS_I_MODIA,
}
