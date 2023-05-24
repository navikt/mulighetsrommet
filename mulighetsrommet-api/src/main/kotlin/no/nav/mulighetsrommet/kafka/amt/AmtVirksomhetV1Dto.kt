package no.nav.mulighetsrommet.kafka.amt

import kotlinx.serialization.Serializable

@Serializable
data class AmtVirksomhetV1Dto(
    val organisasjonsnummer: String,
    val navn: String,
    val overordnetEnhetOrganisasjonsnummer: String?,
)
