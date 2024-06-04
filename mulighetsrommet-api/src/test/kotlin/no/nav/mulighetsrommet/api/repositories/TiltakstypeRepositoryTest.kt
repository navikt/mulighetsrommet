package no.nav.mulighetsrommet.api.repositories

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo
import no.nav.mulighetsrommet.domain.dto.TiltakstypeStatus
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.*

class TiltakstypeRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    afterContainer {
        database.db.truncateAll()
    }

    context("CRUD") {
        test("upsert") {
            val tiltakstyper = TiltakstypeRepository(database.db)

            tiltakstyper.upsert(TiltakstypeFixtures.Arbeidstrening)
            tiltakstyper.upsert(TiltakstypeFixtures.Oppfolging)

            tiltakstyper.getAll().totalCount shouldBe 2
        }
    }

    context("filtrering") {
        val tiltakstyper = TiltakstypeRepository(database.db)
        val tiltakstypeStarterIFremtiden = TiltakstypeDbo(
            id = UUID.randomUUID(),
            navn = "Arbeidsforberedende trening",
            arenaKode = "ARBFORB",
            rettPaaTiltakspenger = true,
            startDato = LocalDate.now().plusDays(1),
            sluttDato = LocalDate.now().plusMonths(1),
        )
        val tiltakstypeHarStartet = TiltakstypeDbo(
            id = UUID.randomUUID(),
            navn = "Jobbklubb",
            arenaKode = "JOBBK",
            rettPaaTiltakspenger = true,
            startDato = LocalDate.now(),
            sluttDato = LocalDate.now().plusMonths(1),
        )
        val tiltakstypeErAvsluttet = TiltakstypeDbo(
            id = UUID.randomUUID(),
            navn = "Oppfølgning",
            arenaKode = "INDOPPFAG",
            rettPaaTiltakspenger = true,
            startDato = LocalDate.now().minusMonths(1),
            sluttDato = LocalDate.now().minusDays(1),
        )
        val idSkalIkkeMigreres = UUID.randomUUID()
        val tiltakstypeSkalIkkeMigreres = TiltakstypeDbo(
            id = idSkalIkkeMigreres,
            navn = "AMOY",
            arenaKode = "AMOY",
            rettPaaTiltakspenger = true,
            startDato = LocalDate.now(),
            sluttDato = LocalDate.now().plusMonths(1),
        )

        tiltakstyper.upsert(tiltakstypeStarterIFremtiden)
        tiltakstyper.upsert(tiltakstypeHarStartet)
        tiltakstyper.upsert(tiltakstypeErAvsluttet)
        tiltakstyper.upsert(tiltakstypeSkalIkkeMigreres)

        test("returnerer bare tiltak som skal migreres") {
            tiltakstyper.getAllSkalMigreres().items shouldHaveSize 3
        }

        test("filtrering på status") {
            forAll(
                row(TiltakstypeStatus.AKTIV, listOf(tiltakstypeStarterIFremtiden.id, tiltakstypeHarStartet.id)),
                row(TiltakstypeStatus.AVSLUTTET, listOf(tiltakstypeErAvsluttet.id)),
            ) { status, expectedIds ->
                val result = tiltakstyper.getAllSkalMigreres(statuser = listOf(status))
                result.totalCount shouldBe expectedIds.size
                result.items.map { it.id } shouldContainExactlyInAnyOrder expectedIds
            }
        }
    }

    test("pagination") {
        val tiltakstyper = TiltakstypeRepository(database.db)

        (1..10).forEach {
            tiltakstyper.upsert(
                TiltakstypeFixtures.Oppfolging.copy(
                    id = UUID.randomUUID(),
                    navn = "$it".padStart(2, '0'),
                    arenaKode = "$it",
                ),
            )
        }

        forAll(
            row(Pagination.all(), 10, "01", "10", 10),
            row(Pagination.of(page = 1, size = 20), 10, "01", "10", 10),
            row(Pagination.of(page = 1, size = 2), 2, "01", "02", 10),
            row(Pagination.of(page = 3, size = 2), 2, "05", "06", 10),
            row(Pagination.of(page = 3, size = 4), 2, "09", "10", 10),
            row(Pagination.of(page = 2, size = 20), 0, null, null, 0),
        ) { pagination, expectedSize, expectedFirst, expectedLast, expectedTotalCount ->
            val (totalCount, items) = tiltakstyper.getAll(pagination)

            items.size shouldBe expectedSize
            items.firstOrNull()?.navn shouldBe expectedFirst
            items.lastOrNull()?.navn shouldBe expectedLast

            totalCount shouldBe expectedTotalCount
        }
    }

    context("Strukturert innhold for deltakerregistrering") {
        val tiltakstyper = TiltakstypeRepository(database.db)

        beforeEach {
            tiltakstyper.upsert(TiltakstypeFixtures.Oppfolging)
            tiltakstyper.upsert(TiltakstypeFixtures.VTA)
            tiltakstyper.upsert(TiltakstypeFixtures.AFT)
        }

        test("Skal hente ut korrekt strukturert innhold for tiltakstype som har strukturert innhold") {
            @Language("PostgreSQL")
            val query = """
                insert into deltaker_registrering_innholdselement(innholdskode, tekst)
                values('jobbsoking', '${Tiltakskode.OPPFOLGING.name}::tiltakskode')
                on conflict do nothing;

                insert into deltaker_registrering_innholdselement(innholdskode, tekst)
                values('kartlegge-helse', '${Tiltakskode.OPPFOLGING.name}::tiltakskode')
                on conflict do nothing;

                update tiltakstype
                set deltaker_registrering_ledetekst = 'Oppfølging er et bra tiltak'
                where tiltakskode = '${Tiltakskode.OPPFOLGING.name}';

                insert into tiltakstype_deltaker_registrering_innholdselement(innholdskode, tiltakskode)
                values('jobbsoking', '${Tiltakskode.OPPFOLGING.name}');

                insert into tiltakstype_deltaker_registrering_innholdselement(innholdskode, tiltakskode)
                values('kartlegge-helse', '${Tiltakskode.OPPFOLGING.name}');
            """.trimIndent()
            database.db.run(queryOf(query).asExecute)

            tiltakstyper.getEksternTiltakstype(TiltakstypeFixtures.Oppfolging.id).shouldNotBeNull().should {
                it.navn shouldBe "Oppfølging"
                it.deltakerRegistreringInnhold?.ledetekst shouldBe "Oppfølging er et bra tiltak"
                it.deltakerRegistreringInnhold?.innholdselementer?.size shouldBe 2
            }
        }

        test("Skal støtte å hente tiltaktype som bare har ledetekst, men ingen innholdselementer") {
            @Language("PostgreSQL")
            val query = """
                update tiltakstype
                set deltaker_registrering_ledetekst = 'VTA er kjempebra'
                where tiltakskode = '${Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET.name}';
            """.trimIndent()
            database.db.run(queryOf(query).asExecute)

            tiltakstyper.getEksternTiltakstype(TiltakstypeFixtures.VTA.id).shouldNotBeNull().should {
                it.navn shouldBe "Varig tilrettelagt arbeid i skjermet virksomhet"
                it.deltakerRegistreringInnhold?.ledetekst shouldBe "VTA er kjempebra"
                it.deltakerRegistreringInnhold?.innholdselementer?.size shouldBe 0
            }
        }

        test("Skal kunne hente tiltakstype uten strukturert innhold for deltakerregistrering") {
            tiltakstyper.getEksternTiltakstype(TiltakstypeFixtures.AFT.id).shouldNotBeNull().should {
                it.deltakerRegistreringInnhold shouldBe null
            }
        }
    }

    test("getBySanityId krasjer ikke") {
        val tiltakstyper = TiltakstypeRepository(database.db)

        tiltakstyper.upsert(TiltakstypeFixtures.Oppfolging)

        val sanityId = UUID.randomUUID()

        @Language("PostgreSQL")
        val query = """
                update tiltakstype
                set sanity_id = '$sanityId'
                where tiltakskode = '${Tiltakskode.OPPFOLGING.name}';
        """.trimIndent()
        database.db.run(queryOf(query).asExecute)

        tiltakstyper.getBySanityId(sanityId).shouldNotBeNull().should {
            it.id shouldBe TiltakstypeFixtures.Oppfolging.id
        }
    }
})
