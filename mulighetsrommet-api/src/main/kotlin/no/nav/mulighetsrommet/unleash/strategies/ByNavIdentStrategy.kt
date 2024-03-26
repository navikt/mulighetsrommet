package no.nav.mulighetsrommet.unleash.strategies

import io.getunleash.UnleashContext
import io.getunleash.strategy.Strategy
import java.util.*

class ByNavIdentStrategy : Strategy {
    companion object {
        const val VALGT_NAVIDENT_PARAM = "valgtNavident"
    }

    override fun getName(): String {
        return "byNavident"
    }

    override fun isEnabled(parameters: MutableMap<String, String>): Boolean {
        return false
    }

    override fun isEnabled(parameters: MutableMap<String, String>, context: UnleashContext): Boolean {
        return context.userId
            .flatMap { userId ->
                Optional.ofNullable(parameters[VALGT_NAVIDENT_PARAM])
                    .map { identer -> identer.split(",\\s?".toRegex()) }
                    .map { enabledeIdenter -> enabledeIdenter.contains(userId) }
            }.orElse(false)
    }
}
