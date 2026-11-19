package no.nav.mulighetsrommet.admin.gjennomforing

import no.nav.mulighetsrommet.api.domain.personalia.AvvistGrunn
import no.nav.mulighetsrommet.validation.FieldError
import java.util.UUID

data class MigrerEnkeltplass(
    val id: UUID,
    val prismodell: UpsertEnkeltplass.Prismodell,
)

sealed interface MigrerEnkeltplassError {
    data class Valideringsfeil(val errors: List<FieldError>) : MigrerEnkeltplassError
    data class ManglerTilgang(val avvistGrunn: AvvistGrunn) : MigrerEnkeltplassError
}
