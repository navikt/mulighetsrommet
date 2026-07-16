package no.nav.mulighetsrommet.admin.utdanning

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.admin.testing.TestAdminDatabase
import no.nav.mulighetsrommet.api.domain.utdanning.Utdanning
import no.nav.mulighetsrommet.api.domain.utdanning.UtdanningsprogramType

class SynkroniserUtdanningerUseCaseTest : FunSpec({
    fun utdanning(
        programomradekode: String = "BABAT1----",
        utdanningId: String = "u1",
        navn: String = "Banemontørfaget (opplæring i bedrift)",
        nusKoder: List<String> = listOf("457103"),
        utdanningslop: List<String> = listOf(programomradekode, "BABAN3----"),
    ) = Utdanning(
        programomradekode = programomradekode,
        utdanningId = utdanningId,
        navn = navn,
        sluttkompetanse = Utdanning.Sluttkompetanse.FAGBREV,
        aktiv = true,
        utdanningstatus = Utdanning.Status.GYLDIG,
        utdanningslop = utdanningslop,
        nusKoder = nusKoder,
    )

    val byggOgAnleggsteknikk = SynkroniserUtdanningerCommand.Programomrade(
        programomradekode = "BABAT1----",
        navn = "Vg1 Bygg- og anleggsteknikk",
        type = UtdanningsprogramType.YRKESFAGLIG,
    )

    context("SynkroniserUtdanningerUseCase") {
        test("sanerer navn på programområder og utdanninger") {
            val db = TestAdminDatabase()
            val useCase = SynkroniserUtdanningerUseCase(db)

            useCase.execute(
                SynkroniserUtdanningerCommand(
                    programomrader = listOf(byggOgAnleggsteknikk),
                    utdanninger = listOf(utdanning()),
                ),
            )

            db.repository.utdanning.findByProgramomradekode("BABAT1----").shouldNotBeNull().should {
                it.navn shouldBe "Bygg- og anleggsteknikk"
                it.utdanninger[0].navn shouldBe "Banemontørfaget"
            }
        }

        test("filtrerer bort utdanninger uten nus-koder") {
            val db = TestAdminDatabase()
            val useCase = SynkroniserUtdanningerUseCase(db)

            useCase.execute(
                SynkroniserUtdanningerCommand(
                    programomrader = listOf(byggOgAnleggsteknikk),
                    utdanninger = listOf(
                        utdanning(utdanningId = "u1", nusKoder = listOf("457103")),
                        utdanning(utdanningId = "u2", nusKoder = emptyList()),
                    ),
                ),
            )

            db.repository.utdanning.findByProgramomradekode("BABAT1----").shouldNotBeNull().should {
                it.utdanninger.map { u -> u.utdanningId } shouldBe listOf("u1")
            }
        }

        test("filtrerer bort programområder som ikke er yrkesfaglige") {
            val db = TestAdminDatabase()
            val useCase = SynkroniserUtdanningerUseCase(db)

            useCase.execute(
                SynkroniserUtdanningerCommand(
                    programomrader = listOf(
                        byggOgAnleggsteknikk,
                        SynkroniserUtdanningerCommand.Programomrade(
                            programomradekode = "STUSF1----",
                            navn = "Vg1 Studiespesialisering",
                            type = UtdanningsprogramType.STUDIEFORBEREDENDE,
                        ),
                    ),
                    utdanninger = listOf(utdanning()),
                ),
            )

            db.repository.utdanning.findByProgramomradekode("BABAT1----").shouldNotBeNull()
            db.repository.utdanning.findByProgramomradekode("STUSF1----").shouldBeNull()
        }

        test("hopper over ukjente programområdekoder uten å feile") {
            val db = TestAdminDatabase()
            val useCase = SynkroniserUtdanningerUseCase(db)

            useCase.execute(
                SynkroniserUtdanningerCommand(
                    programomrader = listOf(
                        SynkroniserUtdanningerCommand.Programomrade(
                            programomradekode = "UKJENT----",
                            navn = "Vg1 Ukjent programområde",
                            type = UtdanningsprogramType.YRKESFAGLIG,
                        ),
                    ),
                    utdanninger = emptyList(),
                ),
            )

            db.repository.utdanning.findByProgramomradekode("UKJENT----").shouldBeNull()
        }
    }
})
