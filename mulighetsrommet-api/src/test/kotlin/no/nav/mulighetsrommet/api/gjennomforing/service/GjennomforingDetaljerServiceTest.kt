package no.nav.mulighetsrommet.api.gjennomforing.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.mockk
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.gjennomforing.api.AdminTiltaksgjennomforingFilter
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtaleDto
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplassDto
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeFeature
import no.nav.mulighetsrommet.api.tiltakstype.service.TiltakstypeService
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.NorskIdentHasher
import no.nav.mulighetsrommet.model.Tiltakskode
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.time.LocalDate
import java.util.UUID

class GjennomforingDetaljerServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val norskIdent = NorskIdent("12345678910")

    val deltaker = DeltakerFixtures.createDeltakerDbo(
        gjennomforingId = GjennomforingFixtures.EnkelAmo.id,
        startDato = LocalDate.of(2025, 1, 1),
        sluttDato = LocalDate.of(2025, 2, 1),
        statusType = DeltakerStatusType.DELTAR,
    )

    val domain = MulighetsrommetTestDomain(
        avtaler = listOf(AvtaleFixtures.oppfolging),
        gjennomforinger = listOf(
            GjennomforingFixtures.Oppfolging1,
            GjennomforingFixtures.EnkelAmo,
        ),
        deltakere = listOf(deltaker),
    ) {
        queries.gjennomforing.setFreeTextSearch(
            GjennomforingFixtures.Oppfolging1.id,
            listOf(GjennomforingFixtures.Oppfolging1.navn),
        )

        queries.gjennomforing.setFreeTextSearch(
            GjennomforingFixtures.EnkelAmo.id,
            listOf(NorskIdentHasher.hash(norskIdent)),
        )
    }

    beforeSpec {
        domain.initialize(database.db)
    }

    fun createService(): GjennomforingDetaljerService {
        val features = Tiltakskode.entries.associateWith { setOf(TiltakstypeFeature.MIGRERT_REDAKSJONELT_INNHOLD) }
        val tiltakstypeService = TiltakstypeService(TiltakstypeService.Config(features), database.db, mockk())
        return GjennomforingDetaljerService(
            db = database.db,
            tiltakstypeService = tiltakstypeService,
            navAnsattService = mockk(),
        )
    }

    context("getGjennomforingDetaljerDto") {
        test("returnerer detaljer for en gruppetiltak-gjennomføring (AVTALE)") {
            val service = createService()

            val dto = service.getGjennomforingDetaljerDto(GjennomforingFixtures.Oppfolging1.id).shouldNotBeNull()

            val gjennomforing = dto.gjennomforing.shouldBeTypeOf<GjennomforingAvtaleDto>()
            gjennomforing.id shouldBe GjennomforingFixtures.Oppfolging1.id
            gjennomforing.navn shouldBe GjennomforingFixtures.Oppfolging1.navn
        }

        test("returnerer detaljer for en enkeltplass-gjennomføring") {
            val service = createService()

            val dto = service.getGjennomforingDetaljerDto(GjennomforingFixtures.EnkelAmo.id).shouldNotBeNull()

            dto.gjennomforing.shouldBeTypeOf<GjennomforingEnkeltplassDto>().id shouldBe GjennomforingFixtures.EnkelAmo.id
        }

        test("returnerer null når gjennomføring ikke finnes") {
            val service = createService()

            service.getGjennomforingDetaljerDto(UUID.randomUUID()).shouldBeNull()
        }
    }

    context("getAllKompaktDto") {
        test("kan søke på navn av gruppetiltak") {
            val service = createService()

            val result = service.getAllKompaktDto(
                pagination = Pagination.all(),
                filter = AdminTiltaksgjennomforingFilter(search = "Oppfølging"),
            )

            result.data shouldHaveSize 1
            result.data.first().id shouldBe GjennomforingFixtures.Oppfolging1.id
        }

        test("kan søke på norskIdent for enkeltplass-gjennomføring") {
            val service = createService()

            val result = service.getAllKompaktDto(
                pagination = Pagination.all(),
                filter = AdminTiltaksgjennomforingFilter(search = norskIdent.value),
            )

            result.data shouldHaveSize 1
            result.data.first().id shouldBe GjennomforingFixtures.EnkelAmo.id
        }

        test("returnerer tomt resultat ved søk som ikke matcher noe") {
            val service = createService()

            val result = service.getAllKompaktDto(
                pagination = Pagination.all(),
                filter = AdminTiltaksgjennomforingFilter(search = "finnesikke"),
            )

            result.data shouldHaveSize 0
        }

        test("returnerer alle gjennomføringer uten søkefilter") {
            val service = createService()

            val result = service.getAllKompaktDto(
                pagination = Pagination.all(),
                filter = AdminTiltaksgjennomforingFilter(),
            )

            result.data shouldHaveSize 2
        }

        test("kan generere excel for gjennomføringer") {
            val service = createService()

            val file = service.exportToExcel(
                pagination = Pagination.all(),
                filter = AdminTiltaksgjennomforingFilter(search = "Oppfølging"),
            )

            println(file.name)

            WorkbookFactory.create(file.inputStream()).use { workbook ->
                val sheet = workbook.getSheetAt(0)

                sheet.getRow(0).getCell(0).stringCellValue shouldBe "Tiltaksnavn"
                sheet.getRow(0).getCell(1).stringCellValue shouldBe "Tiltakstype"

                sheet.lastRowNum shouldBe 1
                sheet.getRow(1).getCell(0).stringCellValue shouldBe "Oppfølging 1"
            }
        }
    }
})
