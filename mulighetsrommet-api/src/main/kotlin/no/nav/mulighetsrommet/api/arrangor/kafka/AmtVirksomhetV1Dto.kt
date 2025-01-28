package no.nav.mulighetsrommet.api.arrangor.kafka

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Organisasjonsnummer

@Serializable
data class AmtVirksomhetV1Dto(
    val organisasjonsnummer: Organisasjonsnummer,
    val navn: String,
    val overordnetEnhetOrganisasjonsnummer: Organisasjonsnummer?,
)
