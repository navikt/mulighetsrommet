package no.nav.mulighetsrommet.api.domain.dbo

data class VirksomhetDbo(
    val organisasjonsnummer: String,
    val navn: String,
    val underenheter: List<UnderenhetDbo>,
)

data class UnderenhetDbo(
    val organisasjonsnummer: String,
    val navn: String,
    val overordnetEnhet: String,
)
