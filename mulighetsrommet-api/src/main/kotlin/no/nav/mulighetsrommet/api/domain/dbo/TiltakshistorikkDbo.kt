package no.nav.mulighetsrommet.api.domain.dbo

import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import java.time.LocalDate
import java.util.*

data class TiltakshistorikkDbo(
    val id: UUID,
    val fraDato: LocalDate? = null,
    val tilDato: LocalDate? = null,
    val status: Deltakerstatus,
    val tiltaksnavn: String?,
    val tiltakstype: String,
    val arrangorOrganisasjonsnummer: String?,
)
