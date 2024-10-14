package no.nav.mulighetsrommet.utdanning.task

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.utdanning.client.UtdanningClient
import no.nav.mulighetsrommet.utdanning.client.UtdanningNoProgramomraade
import no.nav.mulighetsrommet.utdanning.db.UtdanningRepository
import org.intellij.lang.annotations.Language

class SynchronizeUtdanningerTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))
    val utdanningClient: UtdanningClient = mockk(relaxed = true)

    afterTest {
        database.db.truncateAll()
    }

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
        val utdanningRepository = UtdanningRepository(database.db)

        test("Skal synkronisere programområder og utdanninger") {
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

            // TODO: hardkode i service i stedet
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
                it[0].programomrade.navn shouldBe "Bygg- og anleggsteknikk"
                it[0].programomrade.nusKoder shouldBe listOf("3571")
                it[0].utdanninger.size shouldBe 1
                it[0].utdanninger[0].navn shouldBe "Banemontørfaget"
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
                it[0].programomrade.navn shouldBe "Bygg- og anleggsteknikk"
                it[0].programomrade.nusKoder shouldBe listOf("3571")
                it[0].utdanninger.size shouldBe 1
                it[0].utdanninger[0].navn shouldBe "Banemontørfaget"
                it[0].utdanninger[0].nusKoder shouldBe listOf("457103")
                it[0].utdanninger[0].programlopStart shouldBe it[0].programomrade.id
            }
        }
    }
//
//    test("sync integrasjon") {
//        val x = UtdanningClient(config = UtdanningClient.Config(baseUrl = "https://api.utdanning.no"))
//        val synchronizeUtdanninger = SynchronizeUtdanninger(
//            db = database.db,
//            utdanningClient = x,
//            config = SynchronizeUtdanninger.Config(disabled = true, cronPattern = "0 0 0 1 * ?"),
//            slack = mockk(relaxed = true),
//        )
//
//        synchronizeUtdanninger.syncUtdanninger()
//    }
})