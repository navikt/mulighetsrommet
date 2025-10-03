package no.nav.mulighetsrommet.featuretoggle.strategies

import io.getunleash.UnleashContext
import io.getunleash.strategy.Strategy

class ByOrgnrStrategy : Strategy {
    companion object {
        const val VALGT_ORGNR_PARAM = "valgtOrgnr"
    }

    override fun getName(): String {
        return "byOrgnr"
    }

    override fun isEnabled(parameters: MutableMap<String, String>, context: UnleashContext): Boolean {
        val enabledOrgnr = parameters[VALGT_ORGNR_PARAM]
            .takeIf { !it.isNullOrBlank() }
            ?.let { parseOrgnr(it) }
            ?: return false

        return context.getByName(VALGT_ORGNR_PARAM)
            .map { parseOrgnr(it) }
            .map { requestedOrgnr ->
                requestedOrgnr.isNotEmpty() && enabledOrgnr.containsAll(requestedOrgnr)
            }
            .orElse(false)
    }
}
private fun parseOrgnr(it: String) = it
    .split(",")
    .filter { it.isNotBlank() }
