package no.nav.mulighetsrommet.unleash

import io.getunleash.DefaultUnleash
import io.getunleash.Unleash
import io.getunleash.UnleashContext
import io.getunleash.util.UnleashConfig
import no.nav.mulighetsrommet.unleash.strategies.ByNavIdentStrategy
import no.nav.mulighetsrommet.unleash.strategies.ByOrgnrStrategy
import no.nav.mulighetsrommet.unleash.strategies.ByTiltakskodeStrategy

enum class FeatureToggle(val key: String) {
    MULIGHETSROMMET_MIGRERING_OKONOMI_AVBRYT_UTBETALING("mulighetsrommet.migrering.okonomi.avbryt-utbetaling"),
    MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_TILSAGN("mulighetsrommet.tiltakstype.migrering.tilsagn"),
    MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_UTBETALING("mulighetsrommet.tiltakstype.migrering.okonomi"),
    ARRANGORFLATE_OPPRETT_UTBETEALING_INVESTERINGER("arrangorflate.utbetaling.opprett-utbetaling-knapp"),
    ARRANGORFLATE_OPPRETT_UTBETALING_ANNEN_AVTALT_PPRIS("arrangorflate.utbetaling.opprett-utbetaling.annen-avtalt-ppris"),
}

class UnleashService(config: Config) {
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
