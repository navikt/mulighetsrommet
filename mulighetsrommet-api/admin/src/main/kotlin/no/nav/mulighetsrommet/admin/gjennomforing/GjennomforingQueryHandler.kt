package no.nav.mulighetsrommet.admin.gjennomforing

import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingAvtaleDetaljer
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingKompakt
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingType
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.api.shared.PaginatedResult
import no.nav.mulighetsrommet.api.shared.Pagination
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate
import java.util.UUID

interface GjennomforingQueryHandler {
    fun getAll(
        pagination: Pagination = Pagination.all(),
        search: String? = null,
        navEnheter: List<NavEnhetNummer> = emptyList(),
        tiltakstyper: List<UUID> = emptyList(),
        statuser: List<GjennomforingStatusType> = emptyList(),
        sortering: String? = null,
        sluttDatoGreaterThanOrEqualTo: LocalDate? = null,
        avtaleId: UUID? = null,
        arrangorIds: List<UUID> = emptyList(),
        arrangorOrgnr: List<Organisasjonsnummer> = emptyList(),
        administratorNavIdent: NavIdent? = null,
        publisert: Boolean? = null,
        koordinatorNavIdent: NavIdent? = null,
        typer: List<GjennomforingType> = listOf(),
    ): PaginatedResult<GjennomforingKompakt>

    fun getGjennomforingAvtaleDetaljerOrError(id: UUID): GjennomforingAvtaleDetaljer

    fun getAdministratorer(id: UUID): List<GjennomforingAvtaleDetaljer.Administrator>?

    fun getPrismodell(id: UUID): Prismodell?
}
