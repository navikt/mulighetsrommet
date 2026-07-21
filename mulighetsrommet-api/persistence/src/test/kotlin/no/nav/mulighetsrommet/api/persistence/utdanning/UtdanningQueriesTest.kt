package no.nav.mulighetsrommet.api.persistence.utdanning

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.utdanning.Utdanning
import no.nav.mulighetsrommet.api.domain.utdanning.Utdanningsprogram
import no.nav.mulighetsrommet.api.domain.utdanning.UtdanningsprogramType
import no.nav.mulighetsrommet.api.persistence.SqlAdminDatabaseTestListener

class UtdanningQueriesTest : FunSpec({
    val database = extension(SqlAdminDatabaseTestListener())

    fun utdanning(
        programomradekode: String,
        utdanningId: String,
        navn: String,
    ) = Utdanning(
        programomradekode = programomradekode,
        utdanningId = utdanningId,
        navn = navn,
        sluttkompetanse = Utdanning.Sluttkompetanse.FAGBREV,
        aktiv = true,
        utdanningstatus = Utdanning.Status.GYLDIG,
        utdanningslop = listOf(programomradekode, "$utdanningId-vg2"),
        nusKoder = listOf("457103"),
    )

    fun utdanningsprogram(
        programomradekode: String = "BABAT1----",
        navn: String = "Bygg- og anleggsteknikk",
        utdanninger: List<Utdanning> = emptyList(),
    ) = Utdanningsprogram.opprett(
        programomradekode = programomradekode,
        navn = navn,
        type = UtdanningsprogramType.YRKESFAGLIG,
        utdanninger = utdanninger,
    ).getOrNull().shouldNotBeNull()

    test("lagrer og henter utdanningsprogram med tilhørende utdanninger") {
        database.runAndRollback {
            val program = utdanningsprogram(
                utdanninger = listOf(
                    utdanning(programomradekode = "BABAT1----", utdanningId = "u1", navn = "Tømrerfaget"),
                    utdanning(programomradekode = "BABAT1----", utdanningId = "u2", navn = "Betongfaget"),
                ),
            )

            utdanning.save(program)

            utdanning.findByProgramomradekode("BABAT1----").shouldNotBeNull().should {
                it.programomradekode shouldBe "BABAT1----"
                it.navn shouldBe "Bygg- og anleggsteknikk"
                it.type shouldBe UtdanningsprogramType.YRKESFAGLIG
                it.nusKoder shouldBe listOf("3571")
                it.utdanninger.map { it.navn } shouldBe listOf("Tømrerfaget", "Betongfaget")
            }
        }
    }

    test("returnerer null når utdanningsprogram ikke finnes") {
        database.runAndRollback {
            utdanning.findByProgramomradekode("ukjent-kode").shouldBeNull()
        }
    }

    test("oppdaterer eksisterende utdanningsprogram og utdanninger ved nytt lagring (upsert)") {
        database.runAndRollback {
            utdanning.save(
                utdanningsprogram(
                    utdanninger = listOf(
                        utdanning(
                            programomradekode = "BABAT1----",
                            utdanningId = "u1",
                            navn = "Tømrerfaget",
                        ),
                    ),
                ),
            )

            utdanning.save(
                utdanningsprogram(
                    navn = "Bygg- og anleggsteknikk (revidert)",
                    utdanninger = listOf(
                        utdanning(
                            programomradekode = "BABAT1----",
                            utdanningId = "u1",
                            navn = "Tømrerfaget (revidert)",
                        ),
                    ),
                ),
            )

            utdanning.findByProgramomradekode("BABAT1----").shouldNotBeNull().should { it ->
                it.navn shouldBe "Bygg- og anleggsteknikk (revidert)"
                it.utdanninger.map { it.navn } shouldBe listOf("Tømrerfaget (revidert)")
            }
        }
    }

    test("henter id for utdanningsprogram og utdanning") {
        database.runAndRollback {
            val program = utdanningsprogram(
                utdanninger = listOf(
                    utdanning(
                        programomradekode = "BABAT1----",
                        utdanningId = "u1",
                        navn = "Tømrerfaget",
                    ),
                ),
            )

            utdanning.save(program)

            utdanning.findByProgramomradekode("BABAT1----").shouldNotBeNull().should {
                it.id shouldBe program.id
                it.utdanninger.single().id shouldBe program.utdanninger.single().id
            }
        }
    }
})
