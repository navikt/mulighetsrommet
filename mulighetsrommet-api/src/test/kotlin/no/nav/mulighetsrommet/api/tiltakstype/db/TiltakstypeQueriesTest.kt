package no.nav.mulighetsrommet.api.tiltakstype.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.tiltakstype.model.Tiltakstype
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Tiltakskode
import org.intellij.lang.annotations.Language
import java.util.UUID

class TiltakstypeQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    context("CRUD") {
        test("upsert and get") {
            database.runAndRollback {
                queries.tiltakstype.upsert(TiltakstypeFixtures.Arbeidstrening)
                queries.tiltakstype.upsert(TiltakstypeFixtures.Oppfolging)
                queries.tiltakstype.upsert(TiltakstypeFixtures.TilpassetJobbstotte)

                queries.tiltakstype.getAll().size shouldBe 3
            }
        }
    }

    context("filtrering") {
        test("filtrering på tiltakskode") {
            database.runAndRollback {
                queries.tiltakstype.upsert(TiltakstypeFixtures.AFT)
                queries.tiltakstype.upsert(TiltakstypeFixtures.Jobbklubb)
                queries.tiltakstype.upsert(TiltakstypeFixtures.Oppfolging)
                queries.tiltakstype.upsert(TiltakstypeFixtures.EnkelAmo)

                queries.tiltakstype.getAll(
                    tiltakskoder = setOf(
                        Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
                        Tiltakskode.JOBBKLUBB,
                        Tiltakskode.OPPFOLGING,
                    ),
                ) shouldContainExactlyIds listOf(
                    TiltakstypeFixtures.AFT.id,
                    TiltakstypeFixtures.Jobbklubb.id,
                    TiltakstypeFixtures.Oppfolging.id,
                )
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
                    values('jobbsoking', '${Tiltakskode.OPPFOLGING.name}')
                    on conflict do nothing;

                    insert into deltaker_registrering_innholdselement(innholdskode, tekst)
                    values('kartlegge-helse', '${Tiltakskode.OPPFOLGING.name}')
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
})

private infix fun Collection<Tiltakstype>.shouldContainExactlyIds(listOf: Collection<UUID>) {
    map { it.id }.shouldContainExactlyInAnyOrder(listOf)
}
