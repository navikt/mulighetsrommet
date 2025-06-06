package no.nav.mulighetsrommet.unleash

import io.getunleash.DefaultUnleash
import io.getunleash.Unleash
import io.getunleash.UnleashContext
import io.getunleash.util.UnleashConfig
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.unleash.strategies.ByNavIdentStrategy
import no.nav.mulighetsrommet.unleash.strategies.ByOrgnrStrategy
import no.nav.mulighetsrommet.unleash.strategies.ByTiltakskodeStrategy

class UnleashService(config: Config) {
    private val unleash: Unleash

    data class Config(
        val appName: String,
        val url: String,
        val token: String,
        val instanceId: String,
        val environment: String,
    )

    init {
        val unleashConfig = UnleashConfig.builder()
            .appName(config.appName)
            .instanceId(config.instanceId)
            .unleashAPI("${config.url}/api")
            .apiKey(config.token)
            .environment(config.environment)
            .build()

        unleash = DefaultUnleash(
            unleashConfig,
            ByNavIdentStrategy(),
            ByTiltakskodeStrategy(),
            ByOrgnrStrategy(),
        )
    }

    fun isEnabled(feature: String): Boolean {
        return unleash.isEnabled(feature)
    }

    fun isEnabled(feature: String, context: FeatureToggleContext): Boolean {
        val ctx = UnleashContext.builder()
            .userId(context.userId)
            .sessionId(context.sessionId)
            .remoteAddress(context.remoteAddress)
            .addProperty(ByTiltakskodeStrategy.TILTAKSKODER_PARAM, context.tiltakskoder.joinToString(",") { it.name })
            .addProperty(ByOrgnrStrategy.VALGT_ORGNR_PARAM, context.orgnr.joinToString(",") { it.value })
            .build()
        return unleash.isEnabled(feature, ctx)
    }

    fun isEnabledForTiltakstype(toggle: Toggle, vararg tiltakskoder: Tiltakskode): Boolean {
        val ctx = UnleashContext.builder()
            .addProperty(ByTiltakskodeStrategy.TILTAKSKODER_PARAM, tiltakskoder.joinToString(",") { it.name })
            .build()
        return unleash.isEnabled(toggle.featureName, ctx)
    }
}
