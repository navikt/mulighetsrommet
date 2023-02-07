package no.nav.mulighetsrommet.api.routes.v1.responses

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus

@Serializable
data class TiltaksgjennomforingsstatusResponse(
    val status: Tiltaksgjennomforingsstatus
)
