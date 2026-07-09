package no.nav.mulighetsrommet.altinn.model

import no.nav.mulighetsrommet.model.Organisasjonsnummer

data class BedriftRettigheter(
    val organisasjonsnummer: Organisasjonsnummer,
    val rettigheter: List<AltinnRessurs>,
)
