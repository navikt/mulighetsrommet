package no.nav.mulighetsrommet.api.gjennomforing.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.ArrangorFixtures
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.EnkeltplassFixtures.EnkelAmo
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.gjennomforing.model.Enkeltplass
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.GjennomforingStatusType

class EnkeltplassQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    context("CRUD") {
        val domain = MulighetsrommetTestDomain(
            avtaler = listOf(AvtaleFixtures.oppfolging),
        )

        test("lagre enkeltplass") {
            database.runAndRollback { session ->
                domain.setup(session)

                val queries = EnkeltplassQueries(session)

                queries.upsert(EnkelAmo)

                queries.get(EnkelAmo.id) should {
                    it.shouldNotBeNull()
                    it.id shouldBe EnkelAmo.id
                    it.tiltakstype shouldBe Enkeltplass.Tiltakstype(
                        id = TiltakstypeFixtures.EnkelAmo.id,
                        navn = TiltakstypeFixtures.EnkelAmo.navn,
                        tiltakskode = null,
                    )
                    it.arrangor shouldBe Enkeltplass.Arrangor(
                        id = ArrangorFixtures.underenhet1.id,
                        organisasjonsnummer = ArrangorFixtures.underenhet1.organisasjonsnummer,
                        navn = ArrangorFixtures.underenhet1.navn,
                        slettet = false,
                    )
                    it.startDato shouldBe Oppfolging1.startDato
                    it.sluttDato shouldBe Oppfolging1.sluttDato
                    it.arenaAnsvarligEnhet shouldBe null
                    it.status.type shouldBe GjennomforingStatusType.GJENNOMFORES
                    it.opphav shouldBe ArenaMigrering.Opphav.TILTAKSADMINISTRASJON
                }

                queries.delete(Oppfolging1.id)

                queries.get(Oppfolging1.id) shouldBe null
            }
        }
    }
})
