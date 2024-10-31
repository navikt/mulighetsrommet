package no.nav.mulighetsrommet.unleash

import io.getunleash.DefaultUnleash
import io.getunleash.Unleash
import io.getunleash.UnleashContext
import io.getunleash.util.UnleashConfig
import no.nav.common.client.axsys.AxsysClient
import no.nav.mulighetsrommet.unleash.strategies.ByEnhetStrategy
import no.nav.mulighetsrommet.unleash.strategies.ByNavIdentStrategy
import no.nav.mulighetsrommet.unleash.strategies.ByTiltakskodeStrategy

class UnleashService(config: Config, axsysClient: AxsysClient) {
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
            ByEnhetStrategy(axsysClient),
            ByNavIdentStrategy(),
            ByTiltakskodeStrategy(),
        )
    }

    fun isEnabled(feature: String, context: FeatureToggleContext): Boolean {
        val ctx = UnleashContext.builder()
            .userId(context.userId)
            .sessionId(context.sessionId)
            .remoteAddress(context.remoteAddress)
            .addProperty(ByTiltakskodeStrategy.TILTAKSKODER_PARAM, context.tiltakskoder.joinToString(",") { it.name })
            .build()
        return unleash.isEnabled(feature, ctx)
    }

    fun isEnabled(feature: String): Boolean {
        return unleash.isEnabled(feature)
    }
}
