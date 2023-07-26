package no.nav.mulighetsrommet.unleash

import io.getunleash.DefaultUnleash
import io.getunleash.util.UnleashConfig

class UnleashService(config: UnleashConfig) {
    // Skriv noen tester
    // Test at ny unleash funker med en test-toggle for min egen bruker
    private val unleash = DefaultUnleash(config)

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
                .unleashAPI(url).apiKey(token).environment(environment).build()
        }
    }
}
