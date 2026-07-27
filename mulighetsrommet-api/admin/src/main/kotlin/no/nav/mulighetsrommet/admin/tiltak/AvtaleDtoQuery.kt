package no.nav.mulighetsrommet.admin.tiltak

import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.formaterDatoTilEuropeiskDatoformat
import no.nav.mulighetsrommet.spreadsheet.ExcelWorkbookBuilder
import no.nav.mulighetsrommet.spreadsheet.buildExcelWorkbook
import java.io.File
import java.util.UUID
import kotlin.io.path.createTempFile
import kotlin.io.path.outputStream

data class GetAvtaleDto(
    val id: UUID,
)

data class GetAllAvtaleDto(
    val pagination: Pagination = Pagination.all(),
    val filter: AvtaleFilter = AvtaleFilter(),
)

data class GetExcelExport(
    val filter: AvtaleFilter = AvtaleFilter(),
)

data class AvtaleFilter(
    val tiltakskoder: List<Tiltakskode> = emptyList(),
    val search: String? = null,
    val statuser: List<AvtaleStatusType> = emptyList(),
    val avtaletyper: List<Avtaletype> = emptyList(),
    val navEnheter: List<NavEnhetNummer> = emptyList(),
    val sortering: String? = null,
    val arrangorIds: List<UUID> = emptyList(),
    val administratorNavIdent: NavIdent? = null,
    val personvernBekreftet: Boolean? = null,
)

class AvtaleDtoQuery(
    private val db: AdminDatabase,
    private val tiltakstypeService: TiltakstypeService,
) {
    fun execute(query: GetAvtaleDto): AvtaleDto = db.session {
        queries.avtale.getAvtaleDto(query.id) ?: throw NoSuchElementException("Fant ikke avtale med id ${query.id}")
    }

    fun execute(query: GetAllAvtaleDto): PaginatedResult<AvtaleDto> = db.session {
        val tiltakstyper = tiltakstypeService.getIdsByTiltakskoder(query.filter.tiltakskoder)
        queries.avtale.getAllAvtaleDto(
            pagination = query.pagination,
            tiltakstyper = tiltakstyper,
            search = query.filter.search,
            statuser = query.filter.statuser,
            avtaletyper = query.filter.avtaletyper,
            navEnheter = query.filter.navEnheter,
            sortering = query.filter.sortering,
            arrangorIds = query.filter.arrangorIds,
            administratorNavIdent = query.filter.administratorNavIdent,
            personvernBekreftet = query.filter.personvernBekreftet,
        )
    }

    fun execute(query: GetExcelExport): File {
        val avtaler = execute(GetAllAvtaleDto(Pagination.all(), query.filter))

        val workbook = buildExcelWorkbook {
            createAvtalerSheet(avtaler.items)
        }

        return workbook.use {
            val file = createTempFile("avtaler-", ".xlsx")
            file.outputStream().use(it::write)
            file.toFile()
        }
    }
}

private fun ExcelWorkbookBuilder.createAvtalerSheet(
    result: List<AvtaleDto>,
) = table("Avtaler") {
    header(
        "Avtalenavn",
        "Tiltakstype",
        "Avtalenummer",
        "Tiltaksarrangør",
        "Tiltaksarrangør orgnr",
        "Startdato",
        "Sluttdato",
    )

    result.forEach { avtale ->
        row {
            listOf(
                avtale.navn,
                avtale.tiltakstype.navn,
                avtale.avtalenummer,
                avtale.arrangor?.navn,
                avtale.arrangor?.organisasjonsnummer?.value,
                avtale.startDato.formaterDatoTilEuropeiskDatoformat(),
                avtale.sluttDato?.formaterDatoTilEuropeiskDatoformat(),
            )
        }
    }
}
