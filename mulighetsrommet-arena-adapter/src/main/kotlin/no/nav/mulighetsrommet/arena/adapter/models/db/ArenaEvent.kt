package no.nav.mulighetsrommet.arena.adapter.models.db

import kotlinx.serialization.json.JsonElement

data class ArenaEvent(
    val arenaTable: String,
    val arenaId: String,
    val payload: JsonElement,
    val status: ConsumptionStatus,
    val message: String? = null,
    val retries: Int = 0,
) {
    enum class ConsumptionStatus {
        /** Event processing is pending and will be started (or retried) on the next schedule */
        Pending,

        /** Event has been processed */
        Processed,

        /** Processing has failed, event processing can be retried */
        Failed,

        /** Event has been ignored, but is kept in case of future relevance */
        Ignored,

        /** Event payload is invalid and needs manual intervention */
        Invalid,
    }
}
