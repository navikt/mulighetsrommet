package no.nav.mulighetsrommet.featuretoggle.model

import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode

data class FeatureToggleContext(
    val userId: String,
    val sessionId: String,
    val remoteAddress: String,
    val tiltakskoder: List<Tiltakskode>,
    val orgnr: List<Organisasjonsnummer>,
)
