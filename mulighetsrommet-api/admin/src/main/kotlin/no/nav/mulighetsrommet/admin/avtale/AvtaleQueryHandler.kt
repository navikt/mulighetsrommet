package no.nav.mulighetsrommet.admin.avtale

import no.nav.mulighetsrommet.api.shared.PaginatedResult
import no.nav.mulighetsrommet.api.shared.Pagination
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Personopplysning
import java.util.UUID

interface AvtaleQueryHandler {
    fun getAvtaleDto(id: UUID): AvtaleDto?

    fun getAllAvtaleDto(
        pagination: Pagination = Pagination.all(),
        tiltakstyper: List<UUID> = emptyList(),
        search: String? = null,
        statuser: List<AvtaleStatusType> = emptyList(),
        avtaletyper: List<Avtaletype> = emptyList(),
        navEnheter: List<NavEnhetNummer> = emptyList(),
        sortering: String? = null,
        arrangorIds: List<UUID> = emptyList(),
        administratorNavIdent: NavIdent? = null,
        personvernBekreftet: Boolean? = null,
    ): PaginatedResult<AvtaleDto>

    fun getPersonopplysninger(): List<Personopplysning>
}
