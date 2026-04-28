package no.nav.mulighetsrommet.api.tiltakstype.model

import kotlinx.serialization.Serializable

@Serializable
enum class TiltakstypeHandling {
    REDIGER_VEILEDERINFO,
    REDIGER_DELTAKERINFO,
}
