package no.nav.mulighetsrommet.admin.tiltak

import java.util.UUID

sealed interface TiltakstypeUseCaseError {
    data class NotFound(val id: UUID) : TiltakstypeUseCaseError
}
