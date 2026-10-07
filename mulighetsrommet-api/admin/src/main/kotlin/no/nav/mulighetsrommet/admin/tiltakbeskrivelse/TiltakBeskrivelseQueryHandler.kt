package no.nav.mulighetsrommet.admin.tiltakbeskrivelse

import no.nav.mulighetsrommet.api.shared.PaginatedResult
import no.nav.mulighetsrommet.api.shared.Pagination
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

interface TiltakBeskrivelseQueryHandler {
    fun getTiltakBeskrivelseDto(id: UUID): TiltakBeskrivelseDto?

    fun getAllKompaktDto(
        pagination: Pagination = Pagination.all(),
        navEnheter: List<NavEnhetNummer> = emptyList(),
        tiltakstyper: List<Tiltakskode> = emptyList(),
        publisert: Boolean? = null,
        sortering: String? = null,
        administratorNavIdent: NavIdent? = null,
    ): PaginatedResult<TiltakBeskrivelseKompaktDto>

    fun setPublisert(id: UUID, publisert: Boolean)
}
