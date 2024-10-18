package no.nav.mulighetsrommet.altinn.models

import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer

data class BedriftRettigheter(
    val organisasjonsnummer: Organisasjonsnummer,
    val rettigheter: List<AltinnRessurs>,
)
