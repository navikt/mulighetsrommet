package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotliquery.Query
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixture
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.utils.*
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.database.utils.getOrThrow
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.Tiltakstypestatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltakstypeRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))
    val tiltaksgjennomforingFixture = TiltaksgjennomforingFixtures

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
                tilDato = LocalDate.of(2023, 1, 12),
            ),
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
                tilDato = LocalDate.of(2023, 1, 12),
            ),
        )
        Query("update tiltakstype set skal_migreres = true").asUpdate.let { database.db.run(it) }

        tiltakstyper.getAll().second shouldHaveSize 2
        tiltakstyper.getAllSkalMigreres(
            TiltakstypeFilter(
                search = "Førerhund",
                status = Tiltakstypestatus.Aktiv,
                kategori = null,
            ),
        ).second shouldHaveSize 0

        val arbeidstrening =
            tiltakstyper.getAllSkalMigreres(
                TiltakstypeFilter(
                    search = "Arbeidstrening",
                    status = Tiltakstypestatus.Avsluttet,
                    kategori = null,
                ),
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
        database.db.truncateAll()

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
            tilDato = LocalDate.of(2023, 1, 15),
        )
        val tiltakstypeAktiv = TiltakstypeDbo(
            id = UUID.randomUUID(),
            navn = "Jobbklubb",
            tiltakskode = "JOBBK",
            rettPaaTiltakspenger = true,
            registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
            sistEndretDatoIArena = LocalDateTime.of(2022, 1, 15, 0, 0, 0),
            fraDato = LocalDate.of(2023, 1, 11),
            tilDato = LocalDate.of(2023, 1, 15),
        )
        val tiltakstypeAvsluttet = TiltakstypeDbo(
            id = UUID.randomUUID(),
            navn = "Oppfølgning",
            tiltakskode = "INDOPPFOLG",
            rettPaaTiltakspenger = true,
            registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
            sistEndretDatoIArena = LocalDateTime.of(2022, 1, 15, 0, 0, 0),
            fraDato = LocalDate.of(2023, 1, 9),
            tilDato = LocalDate.of(2023, 1, 11),
        )
        val idSkalIkkeMigreres = UUID.randomUUID()
        val tiltakstypeSkalIkkeMigreres = TiltakstypeDbo(
            id = idSkalIkkeMigreres,
            navn = "Oppfølgning",
            tiltakskode = "INDOPPFOLG",
            rettPaaTiltakspenger = true,
            registrertDatoIArena = LocalDateTime.of(2022, 1, 11, 0, 0, 0),
            sistEndretDatoIArena = LocalDateTime.of(2022, 1, 15, 0, 0, 0),
            fraDato = LocalDate.of(2023, 1, 9),
            tilDato = LocalDate.of(2023, 1, 11),
        )

        tiltakstyper.upsert(tiltakstypePlanlagt)
        tiltakstyper.upsert(tiltakstypeAktiv)
        tiltakstyper.upsert(tiltakstypeAvsluttet)
        tiltakstyper.upsert(tiltakstypeSkalIkkeMigreres)
        Query("update tiltakstype set skal_migreres = true where id <> '$idSkalIkkeMigreres'").asUpdate.let {
            database.db.run(
                it,
            )
        }

        test("Filter for kun gruppetiltak returnerer bare gruppetiltak") {
            tiltakstyper.getAllSkalMigreres(
                TiltakstypeFilter(
                    search = null,
                    kategori = Tiltakstypekategori.GRUPPE,
                ),
            ).second shouldHaveSize 2
        }

        test("Filter for kun individuelle tiltak returnerer bare individuelle tiltak") {
            tiltakstyper.getAllSkalMigreres(
                TiltakstypeFilter(
                    search = null,
                    kategori = Tiltakstypekategori.INDIVIDUELL,
                ),
            ).second shouldHaveSize 1
        }

        test("Ingen filter for kategori returnerer både individuelle- og gruppetiltak") {
            tiltakstyper.getAllSkalMigreres(
                TiltakstypeFilter(
                    search = null,
                    kategori = null,
                ),
            ).second shouldHaveSize 3
        }

        test("Filter på planlagt returnerer planlagte tiltakstyper") {
            val typer = tiltakstyper.getAllSkalMigreres(
                TiltakstypeFilter(
                    search = null,
                    kategori = null,
                    status = Tiltakstypestatus.Planlagt,
                    dagensDato = dagensDato,
                ),
            )
            typer.second shouldHaveSize 1
            typer.second.first().id shouldBe tiltakstypePlanlagt.id
        }

        test("Filter på aktiv returnerer aktive tiltakstyper") {
            val typer = tiltakstyper.getAllSkalMigreres(
                TiltakstypeFilter(
                    search = null,
                    kategori = null,
                    status = Tiltakstypestatus.Aktiv,
                    dagensDato = dagensDato,
                ),
            )
            typer.second shouldHaveSize 1
            typer.second.first().id shouldBe tiltakstypeAktiv.id
        }

        test("Filter på avsluttet returnerer avsluttede tiltakstyper") {
            val typer = tiltakstyper.getAllSkalMigreres(
                TiltakstypeFilter(
                    search = null,
                    kategori = null,
                    status = Tiltakstypestatus.Avsluttet,
                    dagensDato = dagensDato,
                ),
            )
            typer.second shouldHaveSize 1
            typer.second.first().id shouldBe tiltakstypeAvsluttet.id
        }
    }

    context("pagination") {
        database.db.truncateAll()

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
                    tilDato = LocalDate.of(2023, 1, 12),
                ),
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
                    20,
                ),
            )

            items.size shouldBe 20
            items.first().navn shouldBe "59"
            items.last().navn shouldBe "76"

            totalCount shouldBe 105
        }

        test("pagination with page 3 default size should give tiltak with id 95-99") {
            val (totalCount, items) = tiltakstyper.getAll(
                paginationParams = PaginationParams(
                    3,
                ),
            )

            items.size shouldBe 5
            items.first().navn shouldBe "95"
            items.last().navn shouldBe "99"

            totalCount shouldBe 105
        }

        test("pagination with default page and size 200 should give tiltak with id 1-99") {
            val (totalCount, items) = tiltakstyper.getAll(
                paginationParams = PaginationParams(
                    nullableLimit = 200,
                ),
            )

            items.size shouldBe 105
            items.first().navn shouldBe "1"
            items.last().navn shouldBe "99"

            totalCount shouldBe 105
        }
    }

    context("Nøkkeltall") {
        val tiltakstypeRepository = TiltakstypeRepository(database.db)
        val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
        val deltakerRepository = DeltakerRepository(database.db)
        val avtaleRepository = AvtaleRepository(database.db)

        test("Skal telle korrekt antall tiltaksgjennomføringer tilknyttet en tiltakstype") {
            val tiltakstypeIdSomIkkeSkalMatche = UUID.randomUUID()

            val gjennomforing1 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                id = UUID.randomUUID(),
                tiltakstypeId = tiltaksgjennomforingFixture.Oppfolging1.tiltakstypeId,
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = LocalDate.of(2022, 10, 15),
            )
            val gjennomforing2 = TiltaksgjennomforingFixtures.Oppfolging2.copy(
                id = UUID.randomUUID(),
                tiltakstypeId = tiltaksgjennomforingFixture.Oppfolging2.tiltakstypeId,
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = LocalDate.of(2050, 10, 15),
            )
            val gjennomforing3 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                id = UUID.randomUUID(),
                tiltakstypeId = tiltakstypeIdSomIkkeSkalMatche,
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = LocalDate.of(2050, 10, 15),
            )
            val gjennomforing4 = TiltaksgjennomforingFixtures.Oppfolging2.copy(
                id = UUID.randomUUID(),
                tiltakstypeId = tiltakstypeIdSomIkkeSkalMatche,
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = LocalDate.of(2050, 10, 15),
            )

            val tiltakstype = TiltakstypeFixtures.Oppfolging.copy(id = gjennomforing1.tiltakstypeId)
            val tiltakstypeUtenGjennomforinger =
                TiltakstypeFixtures.Oppfolging.copy(id = tiltakstypeIdSomIkkeSkalMatche)

            tiltakstypeRepository.upsert(tiltakstype).getOrThrow()
            tiltakstypeRepository.upsert(tiltakstypeUtenGjennomforinger).getOrThrow()

            tiltaksgjennomforingRepository.upsert(gjennomforing1).getOrThrow()
            tiltaksgjennomforingRepository.upsert(gjennomforing2).getOrThrow()
            tiltaksgjennomforingRepository.upsert(gjennomforing3).getOrThrow()
            tiltaksgjennomforingRepository.upsert(gjennomforing4).getOrThrow()

            val antallGjennomforinger = tiltaksgjennomforingRepository.getAll(
                filter = AdminTiltaksgjennomforingFilter(),
            ).getOrThrow()
            antallGjennomforinger.first shouldBe 3

            val antallGjennomforingerForTiltakstype =
                tiltaksgjennomforingRepository.countGjennomforingerForTiltakstypeWithId(tiltakstype.id)
            antallGjennomforingerForTiltakstype shouldBe 1
        }

        test("Skal telle korrekt antall deltakere tilknyttet en avtale") {
            val tiltakstypeIdSomIkkeSkalMatche = UUID.randomUUID()

            val avtale =
                AvtaleFixtures(database).createAvtaleForTiltakstype(tiltakstypeId = tiltaksgjennomforingFixture.Oppfolging1.tiltakstypeId)

            val gjennomforing1 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                id = UUID.randomUUID(),
                tiltakstypeId = tiltaksgjennomforingFixture.Oppfolging1.tiltakstypeId,
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = LocalDate.of(2022, 10, 15),
                avtaleId = avtale.id,
            )
            val gjennomforing2 = TiltaksgjennomforingFixtures.Oppfolging2.copy(
                id = UUID.randomUUID(),
                tiltakstypeId = tiltaksgjennomforingFixture.Oppfolging2.tiltakstypeId,
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = LocalDate.of(2050, 10, 15),
                avtaleId = avtale.id,
            )
            val gjennomforing3 = TiltaksgjennomforingFixtures.Oppfolging1.copy(
                id = UUID.randomUUID(),
                tiltakstypeId = tiltakstypeIdSomIkkeSkalMatche,
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = LocalDate.of(2050, 10, 15),
            )
            val gjennomforing4 = TiltaksgjennomforingFixtures.Oppfolging2.copy(
                id = UUID.randomUUID(),
                tiltakstypeId = tiltakstypeIdSomIkkeSkalMatche,
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = LocalDate.of(2050, 10, 15),
            )

            val deltaker1 =
                DeltakerFixture.Deltaker.copy(tiltaksgjennomforingId = gjennomforing1.id, id = UUID.randomUUID())
            val deltaker2 = DeltakerFixture.Deltaker.copy(
                tiltaksgjennomforingId = gjennomforing1.id,
                id = UUID.randomUUID(),
                startDato = LocalDate.of(2021, 1, 1),
                sluttDato = LocalDate.of(2023, 1, 1),
            )
            val deltaker3 =
                DeltakerFixture.Deltaker.copy(tiltaksgjennomforingId = gjennomforing3.id, id = UUID.randomUUID())
            val deltaker4 =
                DeltakerFixture.Deltaker.copy(tiltaksgjennomforingId = gjennomforing3.id, id = UUID.randomUUID())

            val tiltakstype = TiltakstypeFixtures.Oppfolging.copy(id = gjennomforing1.tiltakstypeId)
            val tiltakstypeUtenGjennomforinger =
                TiltakstypeFixtures.Oppfolging.copy(id = tiltakstypeIdSomIkkeSkalMatche)

            tiltakstypeRepository.upsert(tiltakstype).getOrThrow()
            tiltakstypeRepository.upsert(tiltakstypeUtenGjennomforinger).getOrThrow()

            avtaleRepository.upsert(avtale).getOrThrow()

            tiltaksgjennomforingRepository.upsert(gjennomforing1).getOrThrow()
            tiltaksgjennomforingRepository.upsert(gjennomforing2).getOrThrow()
            tiltaksgjennomforingRepository.upsert(gjennomforing3).getOrThrow()
            tiltaksgjennomforingRepository.upsert(gjennomforing4).getOrThrow()

            deltakerRepository.upsert(deltaker1)
            deltakerRepository.upsert(deltaker2)
            deltakerRepository.upsert(deltaker3)
            deltakerRepository.upsert(deltaker4)

            val antallDeltakereTotalt = deltakerRepository.getAll()
            antallDeltakereTotalt.size shouldBe 4

            val antallDeltakereForAvtale =
                tiltaksgjennomforingRepository.countDeltakereForAvtaleWithId(avtale.id)
            antallDeltakereForAvtale shouldBe 1
        }
    }
})
