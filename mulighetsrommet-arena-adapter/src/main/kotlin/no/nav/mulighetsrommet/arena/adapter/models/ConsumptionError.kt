package no.nav.mulighetsrommet.arena.adapter.models

import io.ktor.client.plugins.*
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.database.utils.DatabaseOperationError

sealed class ConsumptionError(val status: ArenaEvent.ConsumptionStatus, val message: String) {
    data class ProcessingFailed(val details: String) : ConsumptionError(
        status = ArenaEvent.ConsumptionStatus.Failed,
        message = "Event processing failed: $details"
    )

    data class MissingDependency(val details: String) : ConsumptionError(
        status = ArenaEvent.ConsumptionStatus.Failed,
        message = "Dependent event has not yet been processed: $details"
    )

    data class InvalidPayload(val details: String) : ConsumptionError(
        status = ArenaEvent.ConsumptionStatus.Invalid,
        message = "Event payload is invalid: $details"
    )

    data class Ignored(val reason: String) : ConsumptionError(
        status = ArenaEvent.ConsumptionStatus.Ignored,
        message = "Event was ignored: $reason"
    )

    companion object {
        fun fromDatabaseOperationError(error: DatabaseOperationError): ConsumptionError = when (error) {
            is DatabaseOperationError.ForeignKeyViolation -> MissingDependency(error.error.localizedMessage)
            else -> ProcessingFailed(error.error.localizedMessage)
        }

        fun fromResponseException(exception: ResponseException): ConsumptionError {
            return ProcessingFailed(exception.localizedMessage)
        }
    }
}
