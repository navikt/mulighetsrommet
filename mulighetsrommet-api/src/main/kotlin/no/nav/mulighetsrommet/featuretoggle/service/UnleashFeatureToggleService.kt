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

class UnleashFeatureToggleService(config: Config) {
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
        .environment(config.environment)
        .build()

    private val unleash: Unleash = DefaultUnleash(
        unleashConfig,
        ByNavIdentStrategy(),
        ByTiltakskodeStrategy(),
        ByOrgnrStrategy(),
    )

    fun isEnabled(feature: FeatureToggle, context: FeatureToggleContext): Boolean {
        val ctx = UnleashContext.builder()
            .userId(context.userId)
            .sessionId(context.sessionId)
            .remoteAddress(context.remoteAddress)
            .addProperty(ByTiltakskodeStrategy.TILTAKSKODER_PARAM, context.tiltakskoder.joinToString(",") { it.name })
            .addProperty(ByOrgnrStrategy.VALGT_ORGNR_PARAM, context.orgnr.joinToString(",") { it.value })
            .build()

        return unleash.isEnabled(feature.key, ctx)
    }
}
