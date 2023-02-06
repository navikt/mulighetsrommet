package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.utils.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.createApiDatabaseTestSchema
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltakstypeRepositoryTest : FunSpec({

    testOrder = TestCaseOrder.Sequential

    val database = extension(FlywayDatabaseTestListener(createApiDatabaseTestSchema()))

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
                status = Status.AKTIV,
                kategori = null,
                tags = emptyList()
            )
        ).second shouldHaveSize 0

        val arbeidstrening =
            tiltakstyper.getAll(
                TiltakstypeFilter(
                    search = "Arbeidstrening",
                    status = Status.AVSLUTTET,
                    kategori = null,
                    tags = emptyList()
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
        tiltakstyper.upsert(
            TiltakstypeDbo(
                id = UUID.randomUUID(),
                navn = "Arbeidsforberedende trening",
                tiltakskode = "ARBFORB",
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
                navn = "Jobbklubb",
                tiltakskode = "JOBBK",
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

        test("Filter for kun gruppetiltak returnerer bare gruppetiltak") {
            tiltakstyper.getAll(
                TiltakstypeFilter(
                    search = null,
                    status = Status.AVSLUTTET,
                    kategori = Tiltakstypekategori.GRUPPE,
                    tags = emptyList()
                )
            ).second shouldHaveSize 2
        }

        test("Filter for kun individuelle tiltak returnerer bare individuelle tiltak") {
            tiltakstyper.getAll(
                TiltakstypeFilter(
                    search = null,
                    status = Status.AVSLUTTET,
                    kategori = Tiltakstypekategori.INDIVIDUELL,
                    tags = emptyList()
                )
            ).second shouldHaveSize 1
        }

        test("Ingen filter for kategori returnerer både individuelle- og gruppetiltak") {
            tiltakstyper.getAll(
                TiltakstypeFilter(
                    search = null,
                    status = Status.AVSLUTTET,
                    kategori = null,
                    tags = emptyList()
                )
            ).second shouldHaveSize 3
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
