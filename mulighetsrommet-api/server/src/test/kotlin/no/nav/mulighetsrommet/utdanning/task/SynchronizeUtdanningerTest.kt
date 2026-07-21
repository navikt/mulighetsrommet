package no.nav.mulighetsrommet.utdanning.task

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.admin.utdanning.SynkroniserUtdanningerUseCase
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.utdanning.client.UtdanningClient
import no.nav.mulighetsrommet.utdanning.client.UtdanningNoProgramomraade

class SynchronizeUtdanningerTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val utdanningClient: UtdanningClient = mockk(relaxed = true)

    fun createTask() = SynchronizeUtdanninger(
        config = SynchronizeUtdanninger.Config(disabled = true),
        utdanningClient = utdanningClient,
        synkroniserUtdanninger = SynkroniserUtdanningerUseCase(database.admin),
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

            val utdanningsprogram = database.admin.session {
                repository.utdanning.findByProgramomradekode("BABAT1----")
            }.shouldNotBeNull()

            utdanningsprogram should {
                it.navn shouldBe "Bygg- og anleggsteknikk"
                it.utdanninger.size shouldBe 1
                it.utdanninger[0].navn shouldBe "Banemontørfaget"
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

            database.admin.session {
                val programomrade = repository.utdanning.findByProgramomradekode("BABAT1----").shouldNotBeNull()
                programomrade.navn shouldBe "Bygg- og anleggsteknikk"
                programomrade.utdanninger.size shouldBe 1
                programomrade.utdanninger[0].navn shouldBe "Banemontørfaget"

                repository.utdanning.findByProgramomradekode("BABMO2----").shouldBeNull()
            }
        }
    }
})
