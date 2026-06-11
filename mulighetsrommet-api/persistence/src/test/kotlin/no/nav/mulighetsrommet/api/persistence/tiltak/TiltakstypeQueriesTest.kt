package no.nav.mulighetsrommet.api.persistence.tiltak

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.tiltak.Tiltakstype
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.persistence.SqlApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Innholdselement
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

class TiltakstypeQueriesTest : FunSpec({
    val database = extension(SqlApiDatabaseTestListener())

    context("CRUD") {
        test("upsert and get") {
            database.runAndRollback {
                repository.tiltakstype.upsert(TiltakstypeFixtures.Arbeidstrening)
                repository.tiltakstype.upsert(TiltakstypeFixtures.Oppfolging)
                repository.tiltakstype.upsert(TiltakstypeFixtures.VTAO)

                queries.tiltakstype.getAll().size shouldBe 3
            }
        }
    }

    context("filtrering") {
        test("filtrering på tiltakskode") {
            database.runAndRollback {
                repository.tiltakstype.upsert(TiltakstypeFixtures.AFT)
                repository.tiltakstype.upsert(TiltakstypeFixtures.Jobbklubb)
                repository.tiltakstype.upsert(TiltakstypeFixtures.Oppfolging)
                repository.tiltakstype.upsert(TiltakstypeFixtures.EnkelAmo)

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
            database.runAndRollback {
                repository.tiltakstype.upsert(TiltakstypeFixtures.Oppfolging)
                queries.tiltakstype.upsertDeltakerRegistreringInnhold(
                    TiltakstypeFixtures.Oppfolging.id,
                    "Oppfølging er et bra tiltak",
                    listOf("jobbsoking", "arbeidsutproving"),
                )

                queries.tiltakstype.getEksternTiltakstype(TiltakstypeFixtures.Oppfolging.id).shouldNotBeNull().should {
                    it.navn shouldBe "Oppfølging"
                    it.deltakerRegistreringInnhold?.ledetekst shouldBe "Oppfølging er et bra tiltak"
                    it.deltakerRegistreringInnhold?.innholdselementer shouldContainExactlyInAnyOrder listOf(
                        Innholdselement(tekst = "Støtte til å søke jobber", innholdskode = "jobbsoking"),
                        Innholdselement(tekst = "Arbeidsutprøving", innholdskode = "arbeidsutproving"),
                    )
                }
            }
        }

        test("Skal støtte å hente tiltaktype som bare har ledetekst, men ingen innholdselementer") {
            database.runAndRollback {
                repository.tiltakstype.upsert(TiltakstypeFixtures.VTA)
                queries.tiltakstype.upsertDeltakerRegistreringInnhold(
                    TiltakstypeFixtures.VTA.id,
                    "VTA er kjempebra",
                    listOf(),
                )

                queries.tiltakstype.getEksternTiltakstype(TiltakstypeFixtures.VTA.id).shouldNotBeNull().should {
                    it.navn shouldBe "Varig tilrettelagt arbeid i skjermet virksomhet"
                    it.deltakerRegistreringInnhold?.ledetekst shouldBe "VTA er kjempebra"
                    it.deltakerRegistreringInnhold?.innholdselementer?.size shouldBe 0
                }
            }
        }

        test("Skal kunne hente tiltakstype uten strukturert innhold for deltakerregistrering") {
            database.runAndRollback {
                repository.tiltakstype.upsert(TiltakstypeFixtures.AFT)

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
