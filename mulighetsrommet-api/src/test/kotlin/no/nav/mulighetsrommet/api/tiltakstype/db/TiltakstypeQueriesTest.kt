package no.nav.mulighetsrommet.api.tiltakstype.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dto.TiltakstypeStatus
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.*

class TiltakstypeQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    context("CRUD") {
        test("upsert and get") {
            database.runAndRollback { session ->
                val queries = TiltakstypeQueries(session)

                queries.upsert(TiltakstypeFixtures.Arbeidstrening)
                queries.upsert(TiltakstypeFixtures.Oppfolging)

                queries.getAll().size shouldBe 2
            }
        }
    }

    context("filtrering") {
        val tiltakstypeStarterIFremtiden = TiltakstypeFixtures.AFT.copy(
            startDato = LocalDate.now().plusDays(1),
            sluttDato = LocalDate.now().plusMonths(1),
        )
        val tiltakstypeHarStartet = TiltakstypeFixtures.Jobbklubb.copy(
            startDato = LocalDate.now(),
            sluttDato = LocalDate.now().plusMonths(1),
        )
        val tiltakstypeErAvsluttet = TiltakstypeFixtures.Oppfolging.copy(
            startDato = LocalDate.now().minusMonths(1),
            sluttDato = LocalDate.now().minusDays(1),
        )
        val tiltakstypeSkalIkkeMigreres = TiltakstypeFixtures.EnkelAmo.copy(
            startDato = LocalDate.now(),
            sluttDato = LocalDate.now().plusMonths(1),
        )

        test("returnerer bare tiltak som skal migreres") {
            database.runAndRollback { session ->
                val queries = TiltakstypeQueries(session)

                queries.upsert(tiltakstypeStarterIFremtiden)
                queries.upsert(tiltakstypeHarStartet)
                queries.upsert(tiltakstypeErAvsluttet)
                queries.upsert(tiltakstypeSkalIkkeMigreres)

                queries.getAllSkalMigreres() shouldContainExactlyIds listOf(
                    tiltakstypeStarterIFremtiden.id,
                    tiltakstypeHarStartet.id,
                    tiltakstypeErAvsluttet.id,
                )
            }
        }

        test("filtrering på status") {
            database.runAndRollback { session ->
                val queries = TiltakstypeQueries(session)

                queries.upsert(tiltakstypeStarterIFremtiden)
                queries.upsert(tiltakstypeHarStartet)
                queries.upsert(tiltakstypeErAvsluttet)
                queries.upsert(tiltakstypeSkalIkkeMigreres)

                forAll(
                    row(
                        listOf(TiltakstypeStatus.AKTIV),
                        listOf(tiltakstypeStarterIFremtiden.id, tiltakstypeHarStartet.id),
                    ),
                    row(
                        listOf(TiltakstypeStatus.AVSLUTTET),
                        listOf(tiltakstypeErAvsluttet.id),
                    ),
                ) { statuser, expectedIds ->
                    queries.getAllSkalMigreres(statuser = statuser) shouldContainExactlyIds expectedIds
                }
            }
        }
    }

    context("Strukturert innhold for deltakerregistrering") {
        test("Skal hente ut korrekt strukturert innhold for tiltakstype som har strukturert innhold") {
            database.runAndRollback { session ->
                val queries = TiltakstypeQueries(session)

                queries.upsert(TiltakstypeFixtures.Oppfolging)
                queries.upsert(TiltakstypeFixtures.VTA)
                queries.upsert(TiltakstypeFixtures.AFT)

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
                session.execute(queryOf(query))

                queries.getEksternTiltakstype(TiltakstypeFixtures.Oppfolging.id).shouldNotBeNull().should {
                    it.navn shouldBe "Oppfølging"
                    it.deltakerRegistreringInnhold?.ledetekst shouldBe "Oppfølging er et bra tiltak"
                    it.deltakerRegistreringInnhold?.innholdselementer?.size shouldBe 2
                }
            }
        }

        test("Skal støtte å hente tiltaktype som bare har ledetekst, men ingen innholdselementer") {
            database.runAndRollback { session ->
                val queries = TiltakstypeQueries(session)

                queries.upsert(TiltakstypeFixtures.VTA)

                @Language("PostgreSQL")
                val query = """
                update tiltakstype
                set deltaker_registrering_ledetekst = 'VTA er kjempebra'
                where tiltakskode = '${Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET.name}';
                """.trimIndent()
                session.execute(queryOf(query))

                queries.getEksternTiltakstype(TiltakstypeFixtures.VTA.id).shouldNotBeNull().should {
                    it.navn shouldBe "Varig tilrettelagt arbeid i skjermet virksomhet"
                    it.deltakerRegistreringInnhold?.ledetekst shouldBe "VTA er kjempebra"
                    it.deltakerRegistreringInnhold?.innholdselementer?.size shouldBe 0
                }
            }
        }

        test("Skal kunne hente tiltakstype uten strukturert innhold for deltakerregistrering") {
            database.runAndRollback { session ->
                val queries = TiltakstypeQueries(session)

                queries.upsert(TiltakstypeFixtures.AFT)

                queries.getEksternTiltakstype(TiltakstypeFixtures.AFT.id).shouldNotBeNull().should {
                    it.deltakerRegistreringInnhold shouldBe null
                }
            }
        }
    }

    test("getBySanityId krasjer ikke") {
        database.runAndRollback { session ->
            val queries = TiltakstypeQueries(session)

            queries.upsert(TiltakstypeFixtures.Oppfolging)

            val sanityId = UUID.randomUUID()

            @Language("PostgreSQL")
            val query = """
                update tiltakstype
                set sanity_id = '$sanityId'
                where tiltakskode = '${Tiltakskode.OPPFOLGING.name}';
            """.trimIndent()
            session.execute(queryOf(query))

            queries.getBySanityId(sanityId).id shouldBe TiltakstypeFixtures.Oppfolging.id
        }
    }
})

private infix fun Collection<TiltakstypeDto>.shouldContainExactlyIds(listOf: Collection<UUID>) {
    map { it.id }.shouldContainExactlyInAnyOrder(listOf)
}
