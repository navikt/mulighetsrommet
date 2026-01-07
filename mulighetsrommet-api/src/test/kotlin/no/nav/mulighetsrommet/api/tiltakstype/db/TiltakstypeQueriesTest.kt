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
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.TiltakstypeStatus
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.UUID

class TiltakstypeQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    context("CRUD") {
        test("upsert and get") {
            database.runAndRollback {
                queries.tiltakstype.upsert(TiltakstypeFixtures.Arbeidstrening)
                queries.tiltakstype.upsert(TiltakstypeFixtures.Oppfolging)

                queries.tiltakstype.getAll().size shouldBe 2
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
        val tiltakstypeEnkelAmo = TiltakstypeFixtures.EnkelAmo.copy(
            startDato = LocalDate.now(),
            sluttDato = LocalDate.now().plusMonths(1),
        )

        test("filtrering på tiltakskode") {
            database.runAndRollback {
                queries.tiltakstype.upsert(tiltakstypeStarterIFremtiden)
                queries.tiltakstype.upsert(tiltakstypeHarStartet)
                queries.tiltakstype.upsert(tiltakstypeErAvsluttet)
                queries.tiltakstype.upsert(tiltakstypeEnkelAmo)

                queries.tiltakstype.getAll(
                    tiltakskoder = setOf(
                        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
                        Tiltakskode.JOBBKLUBB,
                        Tiltakskode.OPPFOLGING,
                    ),
                ) shouldContainExactlyIds listOf(
                    tiltakstypeStarterIFremtiden.id,
                    tiltakstypeHarStartet.id,
                    tiltakstypeErAvsluttet.id,
                )
            }
        }

        test("filtrering på status") {
            database.runAndRollback {
                queries.tiltakstype.upsert(tiltakstypeStarterIFremtiden)
                queries.tiltakstype.upsert(tiltakstypeHarStartet)
                queries.tiltakstype.upsert(tiltakstypeErAvsluttet)
                queries.tiltakstype.upsert(tiltakstypeEnkelAmo)

                forAll(
                    row(
                        listOf(TiltakstypeStatus.AKTIV),
                        listOf(tiltakstypeStarterIFremtiden.id, tiltakstypeHarStartet.id, tiltakstypeEnkelAmo.id),
                    ),
                    row(
                        listOf(TiltakstypeStatus.AVSLUTTET),
                        listOf(tiltakstypeErAvsluttet.id),
                    ),
                ) { statuser, expectedIds ->
                    queries.tiltakstype.getAll(statuser = statuser) shouldContainExactlyIds expectedIds
                }
            }
        }
    }

    context("Strukturert innhold for deltakerregistrering") {
        test("Skal hente ut korrekt strukturert innhold for tiltakstype som har strukturert innhold") {
            database.runAndRollback { session ->
                queries.tiltakstype.upsert(TiltakstypeFixtures.Oppfolging)
                queries.tiltakstype.upsert(TiltakstypeFixtures.VTA)
                queries.tiltakstype.upsert(TiltakstypeFixtures.AFT)

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

                queries.tiltakstype.getEksternTiltakstype(TiltakstypeFixtures.Oppfolging.id).shouldNotBeNull().should {
                    it.navn shouldBe "Oppfølging"
                    it.deltakerRegistreringInnhold?.ledetekst shouldBe "Oppfølging er et bra tiltak"
                    it.deltakerRegistreringInnhold?.innholdselementer?.size shouldBe 2
                }
            }
        }

        test("Skal støtte å hente tiltaktype som bare har ledetekst, men ingen innholdselementer") {
            database.runAndRollback { session ->
                queries.tiltakstype.upsert(TiltakstypeFixtures.VTA)

                @Language("PostgreSQL")
                val query = """
                update tiltakstype
                set deltaker_registrering_ledetekst = 'VTA er kjempebra'
                where tiltakskode = '${Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET.name}';
                """.trimIndent()
                session.execute(queryOf(query))

                queries.tiltakstype.getEksternTiltakstype(TiltakstypeFixtures.VTA.id).shouldNotBeNull().should {
                    it.navn shouldBe "Varig tilrettelagt arbeid i skjermet virksomhet"
                    it.deltakerRegistreringInnhold?.ledetekst shouldBe "VTA er kjempebra"
                    it.deltakerRegistreringInnhold?.innholdselementer?.size shouldBe 0
                }
            }
        }

        test("Skal kunne hente tiltakstype uten strukturert innhold for deltakerregistrering") {
            database.runAndRollback {
                queries.tiltakstype.upsert(TiltakstypeFixtures.AFT)

                queries.tiltakstype.getEksternTiltakstype(TiltakstypeFixtures.AFT.id).shouldNotBeNull().should {
                    it.deltakerRegistreringInnhold shouldBe null
                }
            }
        }
    }

    test("getBySanityId krasjer ikke") {
        database.runAndRollback { session ->
            queries.tiltakstype.upsert(TiltakstypeFixtures.Oppfolging)

            val sanityId = UUID.randomUUID()

            @Language("PostgreSQL")
            val query = """
                update tiltakstype
                set sanity_id = '$sanityId'
                where tiltakskode = '${Tiltakskode.OPPFOLGING.name}';
            """.trimIndent()
            session.execute(queryOf(query))

            queries.tiltakstype.getBySanityId(sanityId).id shouldBe TiltakstypeFixtures.Oppfolging.id
        }
    }
})

private infix fun Collection<TiltakstypeDto>.shouldContainExactlyIds(listOf: Collection<UUID>) {
    map { it.id }.shouldContainExactlyInAnyOrder(listOf)
}
