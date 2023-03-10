package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.utils.DEFAULT_PAGINATION_LIMIT
import no.nav.mulighetsrommet.api.utils.PaginationParams
import no.nav.mulighetsrommet.api.utils.TiltakstypeFilter
import no.nav.mulighetsrommet.api.utils.Tiltakstypekategori
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.Tiltakstypestatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltakstypeRepositoryTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    test("CRUD") {
        val tiltakstyper = TiltakstypeRepository(database.db)

        tiltakstyper.upsert(
            TiltakstypeDbo(
                id = UUID.randomUUID(),
                navn = "Arbeidstrening",
                tiltakskode = "ARBTREN",
                rettPaaTiltakspenger = true,
                registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
                sistEndretDatoIArena = LocalDateTime.of(2022, 1, 15, 0, 0, 0),
                fraDato = LocalDate.of(2023, 1, 11),
                tilDato = LocalDate.of(2023, 1, 12)
            )
        )
        tiltakstyper.upsert(
            TiltakstypeDbo(
                id = UUID.randomUUID(),
                navn = "Oppfølging",
                tiltakskode = "INDOPPFOLG",
                rettPaaTiltakspenger = true,
                registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
                sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
                fraDato = LocalDate.of(2023, 1, 11),
                tilDato = LocalDate.of(2023, 1, 12)
            )
        )

        tiltakstyper.getAll().second shouldHaveSize 2
        tiltakstyper.getAll(
            TiltakstypeFilter(
                search = "Førerhund",
                status = Tiltakstypestatus.Aktiv,
                kategori = null
            )
        ).second shouldHaveSize 0

        val arbeidstrening =
            tiltakstyper.getAll(
                TiltakstypeFilter(
                    search = "Arbeidstrening",
                    status = Tiltakstypestatus.Avsluttet,
                    kategori = null
                )
            )
        arbeidstrening.second shouldHaveSize 1
        arbeidstrening.second[0].navn shouldBe "Arbeidstrening"
        arbeidstrening.second[0].arenaKode shouldBe "ARBTREN"
        arbeidstrening.second[0].rettPaaTiltakspenger shouldBe true
        arbeidstrening.second[0].registrertIArenaDato shouldBe LocalDateTime.of(2022, 1, 11, 0, 0, 0)
        arbeidstrening.second[0].sistEndretIArenaDato shouldBe LocalDateTime.of(2022, 1, 15, 0, 0, 0)
        arbeidstrening.second[0].fraDato shouldBe LocalDate.of(2023, 1, 11)
        arbeidstrening.second[0].tilDato shouldBe LocalDate.of(2023, 1, 12)
    }

    context("filter") {
        database.db.clean()
        database.db.migrate()

        val tiltakstyper = TiltakstypeRepository(database.db)
        val dagensDato = LocalDate.of(2023, 1, 12)
        val tiltakstypePlanlagt = TiltakstypeDbo(
            id = UUID.randomUUID(),
            navn = "Arbeidsforberedende trening",
            tiltakskode = "ARBFORB",
            rettPaaTiltakspenger = true,
            registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
            sistEndretDatoIArena = LocalDateTime.of(2022, 1, 15, 0, 0, 0),
            fraDato = LocalDate.of(2023, 1, 13),
            tilDato = LocalDate.of(2023, 1, 15)
        )
        val tiltakstypeAktiv = TiltakstypeDbo(
            id = UUID.randomUUID(),
            navn = "Jobbklubb",
            tiltakskode = "JOBBK",
            rettPaaTiltakspenger = true,
            registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
            sistEndretDatoIArena = LocalDateTime.of(2022, 1, 15, 0, 0, 0),
            fraDato = LocalDate.of(2023, 1, 11),
            tilDato = LocalDate.of(2023, 1, 15)
        )
        val tiltakstypeAvsluttet = TiltakstypeDbo(
            id = UUID.randomUUID(),
            navn = "Oppfølgning",
            tiltakskode = "INDOPPFOLG",
            rettPaaTiltakspenger = true,
            registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
            sistEndretDatoIArena = LocalDateTime.of(2022, 1, 15, 0, 0, 0),
            fraDato = LocalDate.of(2023, 1, 9),
            tilDato = LocalDate.of(2023, 1, 11)
        )

        tiltakstyper.upsert(tiltakstypePlanlagt)
        tiltakstyper.upsert(tiltakstypeAktiv)
        tiltakstyper.upsert(tiltakstypeAvsluttet)

        test("Filter for kun gruppetiltak returnerer bare gruppetiltak") {
            tiltakstyper.getAll(
                TiltakstypeFilter(
                    search = null,
                    kategori = Tiltakstypekategori.GRUPPE
                )
            ).second shouldHaveSize 2
        }

        test("Filter for kun individuelle tiltak returnerer bare individuelle tiltak") {
            tiltakstyper.getAll(
                TiltakstypeFilter(
                    search = null,
                    kategori = Tiltakstypekategori.INDIVIDUELL
                )
            ).second shouldHaveSize 1
        }

        test("Ingen filter for kategori returnerer både individuelle- og gruppetiltak") {
            tiltakstyper.getAll(
                TiltakstypeFilter(
                    search = null,
                    kategori = null
                )
            ).second shouldHaveSize 3
        }

        test("Ingen filter for kategori returnerer både individuelle- og gruppetiltak") {
            tiltakstyper.getAll(
                TiltakstypeFilter(
                    search = null,
                    kategori = null
                )
            ).second shouldHaveSize 3
        }

        test("Filter på planlagt returnerer planlagte tiltakstyper") {
            val typer = tiltakstyper.getAll(
                TiltakstypeFilter(
                    search = null,
                    kategori = null,
                    status = Tiltakstypestatus.Planlagt,
                    dagensDato = dagensDato
                )
            )
            typer.second shouldHaveSize 1
            typer.second.first().id shouldBe tiltakstypePlanlagt.id
        }

        test("Filter på aktiv returnerer aktive tiltakstyper") {
            val typer = tiltakstyper.getAll(
                TiltakstypeFilter(
                    search = null,
                    kategori = null,
                    status = Tiltakstypestatus.Aktiv,
                    dagensDato = dagensDato
                )
            )
            typer.second shouldHaveSize 1
            typer.second.first().id shouldBe tiltakstypeAktiv.id
        }

        test("Filter på avsluttet returnerer avsluttede tiltakstyper") {
            val typer = tiltakstyper.getAll(
                TiltakstypeFilter(
                    search = null,
                    kategori = null,
                    status = Tiltakstypestatus.Avsluttet,
                    dagensDato = dagensDato
                )
            )
            typer.second shouldHaveSize 1
            typer.second.first().id shouldBe tiltakstypeAvsluttet.id
        }
    }

    context("pagination") {
        database.db.clean()
        database.db.migrate()

        val tiltakstyper = TiltakstypeRepository(database.db)

        (1..105).forEach {
            tiltakstyper.upsert(
                TiltakstypeDbo(
                    id = UUID.randomUUID(),
                    navn = "$it",
                    tiltakskode = "$it",
                    rettPaaTiltakspenger = true,
                    registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
                    sistEndretDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
                    fraDato = LocalDate.of(2023, 1, 11),
                    tilDato = LocalDate.of(2023, 1, 12)
                )
            )
        }

        test("default pagination gets first 50 tiltak") {
            val (totalCount, items) = tiltakstyper.getAll()

            items.size shouldBe DEFAULT_PAGINATION_LIMIT
            items.first().navn shouldBe "1"
            items.last().navn shouldBe "49"

            totalCount shouldBe 105
        }

        test("pagination with page 4 and size 20 should give tiltak with id 59-76") {
            val (totalCount, items) = tiltakstyper.getAll(
                paginationParams = PaginationParams(
                    4,
                    20
                )
            )

            items.size shouldBe 20
            items.first().navn shouldBe "59"
            items.last().navn shouldBe "76"

            totalCount shouldBe 105
        }

        test("pagination with page 3 default size should give tiltak with id 95-99") {
            val (totalCount, items) = tiltakstyper.getAll(
                paginationParams = PaginationParams(
                    3
                )
            )

            items.size shouldBe 5
            items.first().navn shouldBe "95"
            items.last().navn shouldBe "99"

            totalCount shouldBe 105
        }

        test("pagination with default page and size 200 should give tiltak with id 1-99") {
            val (totalCount, items) = tiltakstyper.getAll(
                paginationParams = PaginationParams(
                    nullableLimit = 200
                )
            )

            items.size shouldBe 105
            items.first().navn shouldBe "1"
            items.last().navn shouldBe "99"

            totalCount shouldBe 105
        }
    }
})
