package no.nav.mulighetsrommet.unleash

import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode

data class FeatureToggleContext(
    val userId: String,
    val sessionId: String,
    val remoteAddress: String,
    val tiltakskoder: List<Tiltakskode>,
    val orgnr: List<Organisasjonsnummer>,
)

enum class Toggle(val featureName: String) {
    MIGRERING_OKONOMI("mulighetsrommet.tiltakstype.migrering.okonomi"),
    MIGRERING_TILSAGN("mulighetsrommet.tiltakstype.migrering.tilsagn"),
}
