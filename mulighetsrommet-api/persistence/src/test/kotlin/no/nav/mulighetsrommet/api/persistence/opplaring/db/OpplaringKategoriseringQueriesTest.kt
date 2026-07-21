package no.nav.mulighetsrommet.api.persistence.opplaring.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.utdanning.Utdanning
import no.nav.mulighetsrommet.api.domain.utdanning.Utdanningsprogram
import no.nav.mulighetsrommet.api.domain.utdanning.UtdanningsprogramType
import no.nav.mulighetsrommet.api.persistence.SqlAdminDatabaseTestListener

class OpplaringKategoriseringQueriesTest : FunSpec({
    val database = extension(SqlAdminDatabaseTestListener())

    fun utdanning(
        programomradekode: String,
        utdanningId: String,
        navn: String,
        nusKoder: List<String> = listOf("457103"),
    ) = Utdanning(
        programomradekode = programomradekode,
        utdanningId = utdanningId,
        navn = navn,
        sluttkompetanse = Utdanning.Sluttkompetanse.FAGBREV,
        aktiv = true,
        utdanningstatus = Utdanning.Status.GYLDIG,
        utdanningslop = listOf(programomradekode, "$utdanningId-vg2"),
        nusKoder = nusKoder,
    )

    test("henter flere utdanningsprogram med tilhørende utdanninger korrekt gruppert") {
        database.runAndRollback {
            val byggOgAnlegg = Utdanningsprogram.opprett(
                programomradekode = "BABAT1----",
                navn = "Bygg- og anleggsteknikk",
                type = UtdanningsprogramType.YRKESFAGLIG,
                utdanninger = listOf(
                    utdanning(programomradekode = "BABAT1----", utdanningId = "u1", navn = "Tømrerfaget"),
                    utdanning(programomradekode = "BABAT1----", utdanningId = "u2", navn = "Betongfaget"),
                ),
            ).getOrNull().shouldNotBeNull()
            repository.utdanning.save(byggOgAnlegg)

            val elektro = Utdanningsprogram.opprett(
                programomradekode = "ELELE1----",
                navn = "Elektro og datateknologi",
                type = UtdanningsprogramType.YRKESFAGLIG,
                utdanninger = listOf(
                    utdanning(programomradekode = "ELELE1----", utdanningId = "u3", navn = "Elektrikerfaget"),
                ),
            ).getOrNull().shouldNotBeNull()
            repository.utdanning.save(elektro)

            queries.opplaering.getUtdanningslop().should { (first, second) ->
                first.utdanningsprogram.navn shouldBe "Bygg- og anleggsteknikk"
                first.utdanninger.map { it.navn } shouldContainExactlyInAnyOrder listOf("Tømrerfaget", "Betongfaget")

                second.utdanningsprogram.navn shouldBe "Elektro og datateknologi"
                second.utdanninger.map { it.navn } shouldContainExactlyInAnyOrder listOf("Elektrikerfaget")
            }
        }
    }

    test("henter utdanningsprogram og utdanninger sortert etter norsk alfabet") {
        database.runAndRollback {
            val aprikos = Utdanningsprogram.opprett(
                programomradekode = "BABAT1----",
                navn = "Aprikosprogrammet",
                type = UtdanningsprogramType.YRKESFAGLIG,
                utdanninger = listOf(
                    utdanning(programomradekode = "BABAT1----", utdanningId = "a1", navn = "Aprikosfaget"),
                    utdanning(programomradekode = "BABAT1----", utdanningId = "a2", navn = "Zeppelinerfaget"),
                    utdanning(programomradekode = "BABAT1----", utdanningId = "a3", navn = "Ærlighetsfaget"),
                    utdanning(programomradekode = "BABAT1----", utdanningId = "a5", navn = "Åpenhetsfaget"),
                ),
            ).getOrNull().shouldNotBeNull()
            repository.utdanning.save(aprikos)

            val zeppeliner = Utdanningsprogram.opprett(
                programomradekode = "ELELE1----",
                navn = "Zeppelinerprogrammet",
                type = UtdanningsprogramType.YRKESFAGLIG,
                utdanninger = listOf(
                    utdanning(programomradekode = "ELELE1----", utdanningId = "z1", navn = "Zeppelinbyggerfaget"),
                ),
            ).getOrNull().shouldNotBeNull()
            repository.utdanning.save(zeppeliner)

            val aerlighet = Utdanningsprogram.opprett(
                programomradekode = "FDFBI1----",
                navn = "Ærlighetsprogrammet",
                type = UtdanningsprogramType.YRKESFAGLIG,
                utdanninger = listOf(
                    utdanning(programomradekode = "FDFBI1----", utdanningId = "ae1", navn = "Ærligfaget"),
                ),
            ).getOrNull().shouldNotBeNull()
            repository.utdanning.save(aerlighet)

            val aapenhet = Utdanningsprogram.opprett(
                programomradekode = "DTDTH1----",
                navn = "Åpenhetsprogrammet",
                type = UtdanningsprogramType.YRKESFAGLIG,
                utdanninger = listOf(
                    utdanning(programomradekode = "DTDTH1----", utdanningId = "aa1", navn = "Åpenfaget"),
                ),
            ).getOrNull().shouldNotBeNull()
            repository.utdanning.save(aapenhet)

            val utdanningslop = queries.opplaering.getUtdanningslop()

            utdanningslop.map { it.utdanningsprogram.navn } shouldBe listOf(
                "Aprikosprogrammet",
                "Zeppelinerprogrammet",
                "Ærlighetsprogrammet",
                "Åpenhetsprogrammet",
            )

            utdanningslop.first().utdanninger.map { it.navn } shouldBe listOf(
                "Aprikosfaget",
                "Zeppelinerfaget",
                "Ærlighetsfaget",
                "Åpenhetsfaget",
            )
        }
    }
})
