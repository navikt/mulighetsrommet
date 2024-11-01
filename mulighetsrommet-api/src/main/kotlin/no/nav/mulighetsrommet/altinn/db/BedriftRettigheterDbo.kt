package no.nav.mulighetsrommet.altinn.db

import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer

data class BedriftRettigheterDbo(
    val organisasjonsnummer: Organisasjonsnummer,
    val rettigheter: List<RettighetDbo>,
)
