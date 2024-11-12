package no.nav.mulighetsrommet.altinn.db

import no.nav.mulighetsrommet.altinn.model.BedriftRettigheter
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import java.time.LocalDateTime

data class PersonBedriftRettigheterDbo(
    val norskIdent: NorskIdent,
    val bedriftRettigheter: List<BedriftRettigheter>,
    val expiry: LocalDateTime,
)
