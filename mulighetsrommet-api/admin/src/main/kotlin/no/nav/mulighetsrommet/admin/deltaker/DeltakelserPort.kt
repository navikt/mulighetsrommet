package no.nav.mulighetsrommet.admin.deltaker

import arrow.core.Either
import no.nav.mulighetsrommet.api.domain.personalia.AvvistGrunn
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.tokenprovider.AccessType
import java.util.UUID

fun interface DeltakelserPort {
    suspend fun finnDeltakerIds(
        norskIdent: NorskIdent,
        accessType: AccessType.OBO.AzureAd,
    ): Either<DeltakelserError, List<UUID>>
}

sealed interface DeltakelserError {
    data class ManglerTilgang(val avvistGrunn: AvvistGrunn) : DeltakelserError
    data object OppslagFeilet : DeltakelserError
}
