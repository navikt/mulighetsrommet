package no.nav.mulighetsrommet.unleash

import io.getunleash.DefaultUnleash
import io.getunleash.util.UnleashConfig
import no.nav.mulighetsrommet.unleash.strategies.ByEnhetStrategy

class UnleashService(config: UnleashConfig, byEnhetStrategy: ByEnhetStrategy) {
    private val unleash = DefaultUnleash(config, byEnhetStrategy)

    fun get(): DefaultUnleash {
        return unleash
    }

    data class Config(
        val appName: String,
        val url: String,
        val token: String,
        val instanceId: String,
        val environment: String,
    ) {
        fun toUnleashConfig(): UnleashConfig {
            return UnleashConfig.builder().appName(appName).instanceId(instanceId)
                .unleashAPI("$url/api").apiKey(token).environment(environment).build()
        }
    }
}
