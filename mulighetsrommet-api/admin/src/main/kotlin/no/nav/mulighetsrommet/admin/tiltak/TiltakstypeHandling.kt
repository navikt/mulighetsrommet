package no.nav.mulighetsrommet.admin.tiltak

import kotlinx.serialization.Serializable

@Serializable
enum class TiltakstypeHandling {
    REDIGER_VEILEDERINFO,
    REDIGER_DELTAKERINFO,
}
