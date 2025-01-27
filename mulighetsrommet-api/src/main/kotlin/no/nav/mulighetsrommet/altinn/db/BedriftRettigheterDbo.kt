package no.nav.mulighetsrommet.altinn.db

import no.nav.mulighetsrommet.model.Organisasjonsnummer

data class BedriftRettigheterDbo(
    val organisasjonsnummer: Organisasjonsnummer,
    val rettigheter: List<RettighetDbo>,
)
