package no.nav.mulighetsrommet.unleash

import no.nav.mulighetsrommet.domain.Tiltakskode

data class FeatureToggleContext(
    val userId: String,
    val sessionId: String,
    val remoteAddress: String,
    val tiltakskoder: List<Tiltakskode>,
)

enum class Toggle(val featureName: String) {
    MIGRERING_OKONOMI("mulighetsrommet.tiltakstype.migrering.okonomi"),
}
