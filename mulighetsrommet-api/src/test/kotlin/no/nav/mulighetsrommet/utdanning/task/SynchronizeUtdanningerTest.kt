package no.nav.mulighetsrommet.utdanning.task

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.Queries
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.utdanning.client.UtdanningClient
import no.nav.mulighetsrommet.utdanning.client.UtdanningNoProgramomraade

class SynchronizeUtdanningerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val utdanningClient: UtdanningClient = mockk(relaxed = true)

    afterTest {
        database.truncateAll()
    }

    fun createTask() = SynchronizeUtdanninger(
        db = database.db,
        utdanningClient = utdanningClient,
        config = SynchronizeUtdanninger.Config(disabled = true, cronPattern = "0 0 0 1 * ?"),
    )

    val utdanningBanemontorfaget = UtdanningNoProgramomraade(
        programomradekode = "BABAN3----",
        utdanningId = "u_banemontorfag",
        navn = "Banemontørfaget (opplæring i bedrift)",
        utdanningsprogram = UtdanningNoProgramomraade.Utdanningsprogram.YRKESFAGLIG,
        sluttkompetanse = UtdanningNoProgramomraade.Sluttkompetanse.Fagbrev,
        aktiv = true,
        utdanningstatus = UtdanningNoProgramomraade.Status.GYLDIG,
        utdanningslop = listOf(
            "BABAT1----",
            "BAANL2----",
            "BABAN3----",
        ),
        nusKodeverk = listOf(
            UtdanningNoProgramomraade.NusKodeverk(
                navn = "Banemontørfaget, Vg3",
                kode = "457103",
            ),
        ),
    )

    val programomradeByggOgAnleggsteknikk = UtdanningNoProgramomraade(
        programomradekode = "BABAT1----",
        utdanningId = null,
        navn = "Vg1 Bygg- og anleggsteknikk",
        utdanningsprogram = UtdanningNoProgramomraade.Utdanningsprogram.YRKESFAGLIG,
        sluttkompetanse = null,
        aktiv = true,
        utdanningstatus = UtdanningNoProgramomraade.Status.GYLDIG,
        utdanningslop = listOf(
            "BABAT1----",
        ),
        nusKodeverk = emptyList(),
    )

    context("Synchronize utdanninger") {
        test("Skal synkronisere programområder og utdanninger") {
            val synchronizeUtdanninger = createTask()

            coEvery { utdanningClient.getUtdanninger() } returns listOf(
                utdanningBanemontorfaget,
                programomradeByggOgAnleggsteknikk,
            )

            synchronizeUtdanninger.syncUtdanninger()

            val programomraderMedUtdanninger = database.run { Queries.utdanning.getUtdanningsprogrammer() }

            programomraderMedUtdanninger should {
                it.size shouldBe 1
                it[0].utdanningsprogram.navn shouldBe "Bygg- og anleggsteknikk"
                it[0].utdanningsprogram.nusKoder shouldBe listOf("3571")
                it[0].utdanninger.size shouldBe 1
                it[0].utdanninger[0].navn shouldBe "Banemontørfaget"
                it[0].utdanninger[0].nusKoder shouldBe listOf("457103")
                it[0].utdanninger[0].programlopStart shouldBe it[0].utdanningsprogram.id
            }
        }

        test("Skal bare synkronisere programområder fra vg1") {
            val synchronizeUtdanninger = createTask()

            val programomradeBetongOgMurVg2 = UtdanningNoProgramomraade(
                programomradekode = "BABMO2----",
                utdanningId = null,
                navn = "Vg2 Betong og mur",
                utdanningsprogram = UtdanningNoProgramomraade.Utdanningsprogram.YRKESFAGLIG,
                sluttkompetanse = null,
                aktiv = true,
                utdanningstatus = UtdanningNoProgramomraade.Status.GYLDIG,
                utdanningslop = listOf(
                    "BABAT1----",
                    "BABMO2----",
                ),
                nusKodeverk = emptyList(),
            )

            coEvery { utdanningClient.getUtdanninger() } returns listOf(
                utdanningBanemontorfaget,
                programomradeByggOgAnleggsteknikk,
                programomradeBetongOgMurVg2,
            )

            synchronizeUtdanninger.syncUtdanninger()

            val programomraderMedUtdanninger = database.run { Queries.utdanning.getUtdanningsprogrammer() }

            programomraderMedUtdanninger should {
                it.size shouldBe 1
                it[0].utdanningsprogram.navn shouldBe "Bygg- og anleggsteknikk"
                it[0].utdanningsprogram.nusKoder shouldBe listOf("3571")
                it[0].utdanninger.size shouldBe 1
                it[0].utdanninger[0].navn shouldBe "Banemontørfaget"
                it[0].utdanninger[0].nusKoder shouldBe listOf("457103")
                it[0].utdanninger[0].programlopStart shouldBe it[0].utdanningsprogram.id
            }
        }
    }
})
