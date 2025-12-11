package no.nav.mulighetsrommet.api.gjennomforing.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.EnkeltplassFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.model.Enkeltplass
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.time.LocalDate

class EnkeltplassQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    context("CRUD") {
        val domain = MulighetsrommetTestDomain(
            avtaler = listOf(),
        )

        val enkelAmo1 = EnkeltplassFixtures.EnkelAmo
        val enkelAmo2 = EnkeltplassFixtures.EnkelAmo2

        test("lagre enkeltplass") {
            database.runAndRollback { session ->
                domain.setup(session)

                queries.gjennomforing.upsert(enkelAmo1)

                queries.enkeltplass.get(enkelAmo1.id).shouldNotBeNull().should {
                    it.id shouldBe enkelAmo1.id
                    it.tiltakstype shouldBe Enkeltplass.Tiltakstype(
                        id = TiltakstypeFixtures.EnkelAmo.id,
                        navn = TiltakstypeFixtures.EnkelAmo.navn,
                        tiltakskode = Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING,
                    )
                    it.arrangor shouldBe Enkeltplass.Arrangor(
                        id = ArrangorFixtures.underenhet1.id,
                        organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                        navn = ArrangorFixtures.underenhet1.navn,
                        slettet = false,
                    )
                    it.arena.shouldBeNull()
                }

                queries.gjennomforing.setArenaData(
                    GjennomforingArenaDataDbo(
                        id = enkelAmo1.id,
                        tiltaksnummer = Tiltaksnummer("2025#1"),
                        navn = "Arena-navn",
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = null,
                        status = GjennomforingStatusType.GJENNOMFORES,
                        arenaAnsvarligEnhet = "0400",
                    ),
                )

                queries.enkeltplass.get(enkelAmo1.id).shouldNotBeNull().arena.shouldNotBeNull().should {
                    it.tiltaksnummer shouldBe Tiltaksnummer("2025#1")
                    it.navn shouldBe "Arena-navn"
                    it.startDato shouldBe LocalDate.of(2025, 1, 1)
                    it.sluttDato.shouldBeNull()
                    it.status shouldBe GjennomforingStatusType.GJENNOMFORES
                    it.ansvarligNavEnhet shouldBe "0400"
                }

                queries.gjennomforing.delete(enkelAmo1.id)

                queries.enkeltplass.get(enkelAmo1.id) shouldBe null
            }
        }

        test("hent alle enkeltplasser") {
            database.runAndRollback { session ->
                domain.setup(session)

                queries.gjennomforing.upsert(enkelAmo1)
                queries.gjennomforing.upsert(enkelAmo2)

                queries.enkeltplass.getAll().totalCount shouldBe 2
                queries.enkeltplass.getAll(tiltakstyper = listOf(TiltakstypeFixtures.EnkelAmo.id)).totalCount shouldBe 2
                queries.enkeltplass.getAll(tiltakstyper = listOf(TiltakstypeFixtures.AFT.id)).totalCount shouldBe 0
            }
        }
    }
})
