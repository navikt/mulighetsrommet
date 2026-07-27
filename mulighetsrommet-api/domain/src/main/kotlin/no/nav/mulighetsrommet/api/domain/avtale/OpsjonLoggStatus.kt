package no.nav.mulighetsrommet.api.domain.avtale

import kotlinx.serialization.Serializable

@Serializable
enum class OpsjonLoggStatus {
    OPSJON_UTLOST,
    SKAL_IKKE_UTLOSE_OPSJON,
}
