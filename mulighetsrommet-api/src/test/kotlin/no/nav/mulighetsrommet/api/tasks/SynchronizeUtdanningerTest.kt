package no.nav.mulighetsrommet.api.tasks

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.utdanning.Utdanning
import no.nav.mulighetsrommet.api.clients.utdanning.UtdanningClient
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.repositories.UtdanningRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import org.intellij.lang.annotations.Language

class SynchronizeUtdanningerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))
    val utdanningClient: UtdanningClient = mockk(relaxed = true)

    afterTest {
        database.db.truncateAll()
    }

    val utdanningBanemontorfaget = Utdanning(
        programomradekode = "BABAN3----",
        utdanningId = "u_banemontorfag",
        navn = "Banemontørfaget (opplæring i bedrift)",
        utdanningsprogram = Utdanning.Utdanningsprogram.YRKESFAGLIG,
        sluttkompetanse = Utdanning.Sluttkompetanse.Fagbrev,
        aktiv = true,
        utdanningstatus = Utdanning.Utdanningstatus.GYLDIG,
        utdanningslop = listOf(
            "BABAT1----",
            "BAANL2----",
            "BABAN3----",
        ),
        nusKodeverk = listOf(
            Utdanning.NusKodeverk(
                navn = "Banemontørfaget, Vg3",
                kode = "457103",
            ),
        ),
    )

    val programomradeByggOgAnleggsteknikk = Utdanning(
        programomradekode = "BABAT1----",
        utdanningId = null,
        navn = "Vg1 Bygg- og anleggsteknikk",
        utdanningsprogram = Utdanning.Utdanningsprogram.YRKESFAGLIG,
        sluttkompetanse = null,
        aktiv = true,
        utdanningstatus = Utdanning.Utdanningstatus.GYLDIG,
        utdanningslop = listOf(
            "BABAT1----",
        ),
        nusKodeverk = emptyList(),
    )

    context("Synchronize utdanninger") {
        test("Skal synkronisere programområder og utdanninger") {

            val utdanningRepository = UtdanningRepository(database.db)

            val synchronizeUtdanninger = SynchronizeUtdanninger(
                db = database.db,
                utdanningClient = utdanningClient,
                config = SynchronizeUtdanninger.Config(disabled = true, cronPattern = "0 0 0 1 * ?"),
                slack = mockk(relaxed = true),
            )

            coEvery { utdanningClient.getUtdanninger() } returns listOf(
                utdanningBanemontorfaget,
                programomradeByggOgAnleggsteknikk,
            )

            synchronizeUtdanninger.syncUtdanninger()

            // Mocker å hardkode nus-kode slik utvikler gjør det i dev og prod for programområdene
            @Language("PostgreSQL")
            val updateNusKoder = """
            update utdanning_programomrade set nus_koder = ARRAY['3571'] where programomradekode = 'BABAT1----';
            """.trimIndent()
            queryOf(
                updateNusKoder,
            ).asExecute.let { database.db.run(it) }

            val programomraderMedUtdanninger = utdanningRepository.getUtdanningerMedProgramomrader()

            programomraderMedUtdanninger should {
                it.size shouldBe 1
                it[0].programomrade.navn shouldBe "Vg1 Bygg- og anleggsteknikk"
                it[0].programomrade.nusKoder shouldBe listOf("3571")
                it[0].utdanninger.size shouldBe 1
                it[0].utdanninger[0].navn shouldBe "Banemontørfaget (opplæring i bedrift)"
                it[0].utdanninger[0].nusKoder shouldBe listOf("457103")
                it[0].utdanninger[0].programlopStart shouldBe it[0].programomrade.id
            }
        }
        test("Skal bare synkronisere programområder fra vg1") {
            val synchronizeUtdanninger = SynchronizeUtdanninger(
                db = database.db,
                utdanningClient = utdanningClient,
                config = SynchronizeUtdanninger.Config(disabled = true, cronPattern = "0 0 0 1 * ?"),
                slack = mockk(relaxed = true),
            )

            val programomradeBetongOgMurVg2 = Utdanning(
                programomradekode = "BABMO2----",
                utdanningId = null,
                navn = "Vg2 Betong og mur",
                utdanningsprogram = Utdanning.Utdanningsprogram.YRKESFAGLIG,
                sluttkompetanse = null,
                aktiv = true,
                utdanningstatus = Utdanning.Utdanningstatus.GYLDIG,
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

            // Mocker å hardkode nus-kode slik utvikler gjør det i dev og prod for programområdene
            @Language("PostgreSQL")
            val updateNusKoder = """
                update utdanning_programomrade set nus_koder = ARRAY['3571'] where programomradekode = 'BABAT1----';
            """.trimIndent()
            queryOf(
                updateNusKoder,
            ).asExecute.let { database.db.run(it) }

            val programomraderMedUtdanninger = utdanningRepository.getUtdanningerMedProgramomrader()

            programomraderMedUtdanninger should {
                it.size shouldBe 1
                it[0].programomrade.navn shouldBe "Vg1 Bygg- og anleggsteknikk"
                it[0].programomrade.nusKoder shouldBe listOf("3571")
                it[0].utdanninger.size shouldBe 1
                it[0].utdanninger[0].navn shouldBe "Banemontørfaget (opplæring i bedrift)"
                it[0].utdanninger[0].nusKoder shouldBe listOf("457103")
                it[0].utdanninger[0].programlopStart shouldBe it[0].programomrade.id
            }
        }
    }
})
