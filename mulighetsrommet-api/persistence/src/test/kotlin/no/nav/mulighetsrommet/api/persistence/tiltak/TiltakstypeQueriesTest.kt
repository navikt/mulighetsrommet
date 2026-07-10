package no.nav.mulighetsrommet.api.persistence.tiltak

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.redaksjoneltinnhold.RedaksjoneltInnholdLenke
import no.nav.mulighetsrommet.api.domain.tiltak.SortDirection
import no.nav.mulighetsrommet.api.domain.tiltak.Tiltakstype
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.persistence.SqlAdminDatabaseTestListener
import no.nav.mulighetsrommet.model.Innholdselement
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

class TiltakstypeQueriesTest : FunSpec({
    val database = extension(SqlAdminDatabaseTestListener())

    context("CRUD") {
        test("upsert and get") {
            database.runAndRollback {
                repository.tiltakstype.save(TiltakstypeFixtures.Arbeidstrening)
                repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
                repository.tiltakstype.save(TiltakstypeFixtures.VTAO)

                repository.tiltakstype.getAll().size shouldBe 3
            }
        }
    }

    context("getAll") {
        test("filtrering på tiltakskode") {
            database.runAndRollback {
                repository.tiltakstype.save(TiltakstypeFixtures.AFT)
                repository.tiltakstype.save(TiltakstypeFixtures.Jobbklubb)
                repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
                repository.tiltakstype.save(TiltakstypeFixtures.EnkelAmo)

                repository.tiltakstype.getAll(
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

        test("sortering på navn") {
            database.runAndRollback {
                repository.tiltakstype.save(TiltakstypeFixtures.AFT)
                repository.tiltakstype.save(TiltakstypeFixtures.Jobbklubb)
                repository.tiltakstype.save(TiltakstypeFixtures.Oppfolging)
                repository.tiltakstype.save(TiltakstypeFixtures.EnkelAmo)

                repository.tiltakstype.getAll(sortDirection = SortDirection.DESC) shouldContainExactlyIds listOf(
                    TiltakstypeFixtures.Oppfolging.id,
                    TiltakstypeFixtures.Jobbklubb.id,
                    TiltakstypeFixtures.EnkelAmo.id,
                    TiltakstypeFixtures.AFT.id,
                )
            }
        }
    }

    context("hent TiltakstypeV3Dto") {
        test("Skal hente ut korrekt strukturert innhold for tiltakstype som har strukturert innhold") {
            database.runAndRollback {
                repository.tiltakstype.save(
                    TiltakstypeFixtures.Oppfolging.copy(
                        deltakerinfo = Tiltakstype.Deltakerinfo(
                            "Oppfølging er et bra tiltak",
                            listOf("jobbsoking", "arbeidsutproving"),
                        ),
                    ),
                )

                val eksternTiltakstype = tiltakstype.getEksternTiltakstype(TiltakstypeFixtures.Oppfolging.id)

                eksternTiltakstype.shouldNotBeNull().should {
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
                repository.tiltakstype.save(
                    TiltakstypeFixtures.VTA.copy(
                        deltakerinfo = Tiltakstype.Deltakerinfo(
                            "VTA er kjempebra",
                            listOf(),
                        ),
                    ),
                )

                val eksternTiltakstype = tiltakstype.getEksternTiltakstype(TiltakstypeFixtures.VTA.id)

                eksternTiltakstype.shouldNotBeNull().should {
                    it.navn shouldBe "Varig tilrettelagt arbeid i skjermet virksomhet"
                    it.deltakerRegistreringInnhold?.ledetekst shouldBe "VTA er kjempebra"
                    it.deltakerRegistreringInnhold?.innholdselementer.shouldBeEmpty()
                }
            }
        }

        test("Skal kunne hente tiltakstype uten strukturert innhold for deltakerregistrering") {
            database.runAndRollback {
                repository.tiltakstype.save(TiltakstypeFixtures.AFT)

                val eksternTiltakstype = tiltakstype.getEksternTiltakstype(TiltakstypeFixtures.AFT.id)

                eksternTiltakstype.shouldNotBeNull().should {
                    it.deltakerRegistreringInnhold shouldBe null
                }
            }
        }
    }

    context("getNamesReferencingLenke") {
        test("returnerer tom liste når ingen tiltakstyper bruker lenken") {
            database.runAndRollback {
                val lenke = RedaksjoneltInnholdLenke(id = UUID.randomUUID(), url = "https://nav.no", navn = null)
                repository.redaksjoneltInnholdLenke.upsert(lenke)

                queries.tiltakstype.getNamesReferencingLenke(lenke.id).shouldBeEmpty()
            }
        }

        test("returnerer navn på tiltakstyper som har lenken som faglenke, sortert alfabetisk") {
            database.runAndRollback {
                val lenke = RedaksjoneltInnholdLenke(id = UUID.randomUUID(), url = "https://nav.no", navn = null)
                repository.redaksjoneltInnholdLenke.upsert(lenke)

                val veilederinfo = Tiltakstype.Veilederinfo(
                    faglenker = listOf(lenke.id),
                )
                repository.tiltakstype.save(
                    TiltakstypeFixtures.AFT.copy(veilederinfo = veilederinfo),
                )
                repository.tiltakstype.save(
                    TiltakstypeFixtures.VTA.copy(veilederinfo = veilederinfo),
                )

                queries.tiltakstype.getNamesReferencingLenke(lenke.id) shouldBe listOf(
                    "Arbeidsforberedende trening",
                    "Varig tilrettelagt arbeid i skjermet virksomhet",
                )
            }
        }
    }
})

private infix fun Collection<Tiltakstype>.shouldContainExactlyIds(listOf: Collection<UUID>) {
    map { it.id }.shouldContainExactlyInAnyOrder(listOf)
}
