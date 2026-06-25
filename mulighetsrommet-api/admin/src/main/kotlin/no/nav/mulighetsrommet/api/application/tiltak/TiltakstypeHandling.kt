package no.nav.mulighetsrommet.api.application.tiltak

import kotlinx.serialization.Serializable

@Serializable
enum class TiltakstypeHandling {
    REDIGER_VEILEDERINFO,
    REDIGER_DELTAKERINFO,
}
