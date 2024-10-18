package no.nav.mulighetsrommet.kafka.consumers.amt

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer

@Serializable
data class AmtVirksomhetV1Dto(
    val organisasjonsnummer: Organisasjonsnummer,
    val navn: String,
    val overordnetEnhetOrganisasjonsnummer: Organisasjonsnummer?,
)
