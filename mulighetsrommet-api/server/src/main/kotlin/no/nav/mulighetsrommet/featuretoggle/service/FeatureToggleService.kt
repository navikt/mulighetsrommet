package no.nav.mulighetsrommet.featuretoggle.service

import no.nav.mulighetsrommet.featuretoggle.model.FeatureToggle
import no.nav.mulighetsrommet.featuretoggle.model.FeatureToggleContext
import no.nav.mulighetsrommet.model.Tiltakskode

interface FeatureToggleService {
    fun isEnabled(feature: FeatureToggle): Boolean

    fun isEnabled(feature: FeatureToggle, context: FeatureToggleContext): Boolean = isEnabled(feature)

    fun isEnabledForTiltakstype(feature: FeatureToggle, vararg tiltakskoder: Tiltakskode): Boolean = isEnabled(feature)
}
