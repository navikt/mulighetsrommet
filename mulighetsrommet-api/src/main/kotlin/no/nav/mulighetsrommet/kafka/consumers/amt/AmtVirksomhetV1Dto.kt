package no.nav.mulighetsrommet.kafka.consumers.amt

import kotlinx.serialization.Serializable

@Serializable
data class AmtVirksomhetV1Dto(
    val organisasjonsnummer: String,
    val navn: String,
    val overordnetEnhetOrganisasjonsnummer: String?,
)
