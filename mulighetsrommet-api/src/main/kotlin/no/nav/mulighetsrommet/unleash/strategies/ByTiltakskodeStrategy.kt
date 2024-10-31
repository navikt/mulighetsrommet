package no.nav.mulighetsrommet.unleash.strategies

import io.getunleash.UnleashContext
import io.getunleash.strategy.Strategy
import no.nav.mulighetsrommet.domain.Tiltakskode

class ByTiltakskodeStrategy : Strategy {
    companion object {
        const val TILTAKSKODER_PARAM = "tiltakskoder"
    }

    override fun getName(): String {
        return "by-tiltakskode"
    }

    override fun isEnabled(parameters: MutableMap<String, String>): Boolean {
        return false
    }

    override fun isEnabled(parameters: MutableMap<String, String>, context: UnleashContext): Boolean {
        val enabledTiltakskoder = parameters[TILTAKSKODER_PARAM]
            .takeIf { !it.isNullOrBlank() }
            ?.let { parseTiltakskoder(it) }
            ?: return false

        return context.getByName(TILTAKSKODER_PARAM)
            .map { parseTiltakskoder(it) }
            .map { requestedTiltakskoder ->
                enabledTiltakskoder.containsAll(requestedTiltakskoder)
            }
            .orElse(false)
    }
}

private fun parseTiltakskoder(it: String) = it.split(",").map { Tiltakskode.valueOf(it) }
