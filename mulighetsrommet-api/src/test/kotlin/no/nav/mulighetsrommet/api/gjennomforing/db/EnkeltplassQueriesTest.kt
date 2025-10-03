package no.nav.mulighetsrommet.api.gjennomforing.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.EnkeltplassFixtures.EnkelAmo
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.model.Enkeltplass
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate

class EnkeltplassQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    context("CRUD") {
        val domain = MulighetsrommetTestDomain(
            avtaler = listOf(),
        )

        test("lagre enkeltplass") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = EnkeltplassQueries(session)

                queries.upsert(EnkelAmo)

                queries.get(EnkelAmo.id).shouldNotBeNull().should {
                    it.id shouldBe EnkelAmo.id
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

                queries.setArenaData(
                    EnkeltplassArenaDataDbo(
                        id = EnkelAmo.id,
                        tiltaksnummer = "2025#1",
                        navn = "Arena-navn",
                        startDato = LocalDate.of(2025, 1, 1),
                        sluttDato = null,
                        status = GjennomforingStatusType.GJENNOMFORES,
                        arenaAnsvarligEnhet = "0400",
                    ),
                )

                queries.get(EnkelAmo.id).shouldNotBeNull().arena.shouldNotBeNull().should {
                    it.tiltaksnummer shouldBe "2025#1"
                    it.navn shouldBe "Arena-navn"
                    it.startDato shouldBe LocalDate.of(2025, 1, 1)
                    it.sluttDato.shouldBeNull()
                    it.status shouldBe GjennomforingStatusType.GJENNOMFORES
                    it.arenaAnsvarligEnhet shouldBe "0400"
                }

                queries.delete(EnkelAmo.id)

                queries.get(EnkelAmo.id) shouldBe null
            }
        }
    }
})
