package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.domain.dbo.UtkastDbo
import no.nav.mulighetsrommet.api.domain.dto.UtkastDto
import no.nav.mulighetsrommet.api.repositories.UtkastRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.NotFound
import no.nav.mulighetsrommet.api.routes.v1.responses.ServerError
import no.nav.mulighetsrommet.api.routes.v1.responses.StatusResponse
import java.util.*

class UtkastService(
    private val utkastRepository: UtkastRepository,
) {
    fun get(id: UUID): StatusResponse<UtkastDto> {
        return utkastRepository.get(id).map { it!! }.mapLeft { NotFound("Fant ingen utkast med id: $id") }
    }

    fun upsert(utkast: UtkastDbo): StatusResponse<UtkastDto> {
        return utkastRepository.upsert(utkast).map { it!! }
            .mapLeft { error -> ServerError("Klarte ikke lagre utkast med id: ${utkast.id}. Cause: ${error?.toString()}") }
    }

    fun deleteUtkast(id: UUID): StatusResponse<Unit> {
        return utkastRepository.delete(id).map {}
            .mapLeft {
                ServerError(message = "Det oppsto en feil ved sletting av utkastet")
            }
    }
}
