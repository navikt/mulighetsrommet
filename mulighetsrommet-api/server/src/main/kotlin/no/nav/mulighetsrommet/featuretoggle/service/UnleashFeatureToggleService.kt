package no.nav.mulighetsrommet.featuretoggle.service

import io.getunleash.DefaultUnleash
import io.getunleash.Unleash
import io.getunleash.UnleashContext
import io.getunleash.util.UnleashConfig
import no.nav.mulighetsrommet.featuretoggle.model.FeatureToggle
import no.nav.mulighetsrommet.featuretoggle.model.FeatureToggleContext
import no.nav.mulighetsrommet.featuretoggle.strategies.ByNavIdentStrategy
import no.nav.mulighetsrommet.featuretoggle.strategies.ByOrgnrStrategy
import no.nav.mulighetsrommet.featuretoggle.strategies.ByTiltakskodeStrategy
import no.nav.mulighetsrommet.model.Tiltakskode

class UnleashFeatureToggleService(config: Config) : FeatureToggleService {
    data class Config(
        val appName: String,
        val url: String,
        val token: String,
        val instanceId: String,
        val environment: String,
    )

    private val unleashConfig: UnleashConfig = UnleashConfig.builder()
        .appName(config.appName)
        .instanceId(config.instanceId)
        .unleashAPI("${config.url}/api")
        .apiKey(config.token)
        .build()

    private val unleash: Unleash = DefaultUnleash(
        unleashConfig,
        ByNavIdentStrategy(),
        ByTiltakskodeStrategy(),
        ByOrgnrStrategy(),
    )

    override fun isEnabled(feature: FeatureToggle): Boolean {
        return unleash.isEnabled(feature.key)
    }

    override fun isEnabled(feature: FeatureToggle, context: FeatureToggleContext): Boolean {
        val ctx = UnleashContext.builder()
            .apply { context.userId?.let { userId(it) } }
            .apply { context.sessionId?.let { sessionId(it) } }
            .apply { context.remoteAddress?.let { remoteAddress(it) } }
            .addProperty(ByTiltakskodeStrategy.TILTAKSKODER_PARAM, context.tiltakskoder.joinToString(",") { it.name })
            .addProperty(ByOrgnrStrategy.VALGT_ORGNR_PARAM, context.orgnr.joinToString(",") { it.value })
            .build()

        return unleash.isEnabled(feature.key, ctx)
    }

    override fun isEnabledForTiltakstype(feature: FeatureToggle, vararg tiltakskoder: Tiltakskode): Boolean {
        val ctx = UnleashContext.builder()
            .addProperty(ByTiltakskodeStrategy.TILTAKSKODER_PARAM, tiltakskoder.joinToString(",") { it.name })
            .build()
        return unleash.isEnabled(feature.key, ctx)
    }
}
