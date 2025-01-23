package no.nav.mulighetsrommet.arena.adapter.models

import io.ktor.client.plugins.*
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.database.utils.DatabaseOperationError
import no.nav.mulighetsrommet.database.utils.IntegrityConstraintViolation

sealed class ProcessingError(val status: ArenaEvent.ProcessingStatus, val message: String) {
    data class ProcessingFailed(val details: String) : ProcessingError(
        status = ArenaEvent.ProcessingStatus.Failed,
        message = "Event processing failed: $details",
    )

    data class ForeignKeyViolation(val details: String) : ProcessingError(
        status = ArenaEvent.ProcessingStatus.Failed,
        message = "Dependent event has not yet been processed: $details",
    )

    companion object {
        fun fromDatabaseOperationError(error: DatabaseOperationError): ProcessingError = when (error) {
            is IntegrityConstraintViolation.ForeignKeyViolation -> ForeignKeyViolation(error.error.localizedMessage)

            else -> ProcessingFailed(error.error.localizedMessage)
        }

        fun fromResponseException(exception: ResponseException): ProcessingError {
            return ProcessingFailed(exception.localizedMessage)
        }
    }
}

data class ProcessingResult(
    val status: ArenaEntityMapping.Status,
    val message: String? = null,
)
