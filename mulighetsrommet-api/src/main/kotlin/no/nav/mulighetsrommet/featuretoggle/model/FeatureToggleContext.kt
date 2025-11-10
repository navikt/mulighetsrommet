package no.nav.mulighetsrommet.featuretoggle.model

import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode

data class FeatureToggleContext(
    val userId: String? = null,
    val sessionId: String? = null,
    val remoteAddress: String? = null,
    val tiltakskoder: List<Tiltakskode> = listOf(),
    val orgnr: List<Organisasjonsnummer> = listOf(),
)
