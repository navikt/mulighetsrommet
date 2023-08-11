package no.nav.mulighetsrommet.unleash

data class FeatureToggleContext(
    val userId: String,
    val sessionId: String,
    val remoteAddress: String,
)
