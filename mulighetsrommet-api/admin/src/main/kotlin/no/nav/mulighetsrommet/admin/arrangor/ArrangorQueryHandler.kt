package no.nav.mulighetsrommet.admin.arrangor

import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorKontaktperson
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.util.UUID

interface ArrangorQueryHandler {
    fun getAll(
        kobling: ArrangorKobling? = null,
        sok: String? = null,
        overordnetEnhetOrgnr: Organisasjonsnummer? = null,
        slettet: Boolean? = null,
        utenlandsk: Boolean? = null,
        pagination: Pagination = Pagination.all(),
        sortering: String? = null,
    ): PaginatedResult<ArrangorDto>

    fun get(orgnr: Organisasjonsnummer): ArrangorDto?

    fun getById(id: UUID): ArrangorDto

    fun getHovedenhetById(id: UUID): ArrangorDto

    fun koblingerTilKontaktperson(
        kontaktpersonId: UUID,
    ): Pair<List<DokumentKoblingForKontaktperson>, List<DokumentKoblingForKontaktperson>>

    fun getKontaktpersoner(arrangorId: UUID): List<ArrangorKontaktperson>
}
