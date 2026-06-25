package no.nav.mulighetsrommet.api.application.tiltak

import java.util.UUID

sealed interface TiltakstypeUseCaseError {
    data class NotFound(val id: UUID) : TiltakstypeUseCaseError
}
