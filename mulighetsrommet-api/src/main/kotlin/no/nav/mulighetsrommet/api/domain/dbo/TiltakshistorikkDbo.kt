package no.nav.mulighetsrommet.api.domain.dbo

import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import java.time.LocalDateTime
import java.util.*

data class TiltakshistorikkDbo(
    val id: UUID,
    val fraDato: LocalDateTime? = null,
    val tilDato: LocalDateTime? = null,
    val status: Deltakerstatus,
    val tiltaksnavn: String?,
    val tiltakstype: String,
    val arrangorOrganisasjonsnummer: String?,
)
