package no.nav.mulighetsrommet.admin.tiltakdokument

import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

interface TiltakDokumentQueryHandler {
    fun getTiltakDokumentDto(id: UUID): TiltakDokumentDto?

    fun getAllKompaktDto(
        pagination: Pagination = Pagination.all(),
        navEnheter: List<NavEnhetNummer> = emptyList(),
        tiltakstyper: List<Tiltakskode> = emptyList(),
        publisert: Boolean? = null,
        sortering: String? = null,
    ): PaginatedResult<TiltakDokumentKompaktDto>

    fun setPublisert(id: UUID, publisert: Boolean)
}
