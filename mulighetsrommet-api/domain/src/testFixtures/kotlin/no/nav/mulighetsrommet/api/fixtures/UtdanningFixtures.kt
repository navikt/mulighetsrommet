package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.utdanning.Utdanning
import no.nav.mulighetsrommet.api.domain.utdanning.Utdanningsprogram
import no.nav.mulighetsrommet.api.domain.utdanning.UtdanningsprogramType
import java.util.UUID

object UtdanningFixtures {
    object Utdanningsprogrammer {
        val byggOgAnlegg = Utdanningsprogram.fromStorage(
            id = UUID.fromString("1390a963-e9b2-4677-bb87-243f4638b7a1"),
            programomradekode = "BABAT1----",
            navn = "Bygg- og anleggsteknikk",
            type = UtdanningsprogramType.YRKESFAGLIG,
            nusKoder = listOf("3571"),
            utdanninger = listOf(
                Utdanninger.fjellOgBergverksfaget,
                Utdanninger.banemontorfaget,
                Utdanninger.byggdrifterfaget,
            ),
        )

        val handVerkDesignOgProduktutvikling = Utdanningsprogram.fromStorage(
            id = UUID.fromString("1626096d-f1ac-4c34-aa93-741503bc5584"),
            programomradekode = "DTDTH1----",
            navn = "Håndverk, design og produktutvikling",
            type = UtdanningsprogramType.YRKESFAGLIG,
            nusKoder = listOf("3169"),
            utdanninger = listOf(
                Utdanninger.glassblaserfaget,
                Utdanninger.bunadstilvirkerfaget,
                Utdanninger.gjortlerfaget,
            ),
        )
    }

    object Utdanninger {
        val fjellOgBergverksfaget = Utdanning(
            id = UUID.fromString("c8ac69fa-6ba6-494f-95e1-6ec9be06086d"),
            programomradekode = "BABAT1----",
            utdanningId = "u_fjell_bergverksfag",
            navn = "Fjell- og bergverksfaget",
            sluttkompetanse = Utdanning.Sluttkompetanse.FAGBREV,
            aktiv = true,
            utdanningstatus = Utdanning.Status.GYLDIG,
            utdanningslop = listOf("BABAT1----", "BAANL2----", "BAFJE3----"),
            nusKoder = listOf("458409"),
        )

        val banemontorfaget = Utdanning(
            id = UUID.fromString("d02ffbea-7f0e-42ff-91a0-88d56277699d"),
            programomradekode = "BABAT1----",
            utdanningId = "u_banemontorfag",
            navn = "Banemontørfaget",
            sluttkompetanse = Utdanning.Sluttkompetanse.FAGBREV,
            aktiv = true,
            utdanningstatus = Utdanning.Status.GYLDIG,
            utdanningslop = listOf("BABAT1----", "BAANL2----", "BABAN3----"),
            nusKoder = listOf("457103"),
        )

        val byggdrifterfaget = Utdanning(
            id = UUID.fromString("32fff4d1-ef8f-4990-8184-7a7b34febe3b"),
            programomradekode = "BABAT1----",
            utdanningId = "u_byggdrifterfag",
            navn = "Byggdrifterfaget",
            sluttkompetanse = Utdanning.Sluttkompetanse.FAGBREV,
            aktiv = true,
            utdanningstatus = Utdanning.Status.GYLDIG,
            utdanningslop = listOf("BABAT1----", "BABDR3----"),
            nusKoder = listOf("457136"),
        )

        val glassblaserfaget = Utdanning(
            id = UUID.fromString("7a7a7c50-4800-4e97-be97-f4a87216c8f5"),
            programomradekode = "DTDTH1----",
            utdanningId = "u_glasshandverkerfag",
            navn = "Glassblåserfaget",
            sluttkompetanse = Utdanning.Sluttkompetanse.FAGBREV,
            aktiv = true,
            utdanningstatus = Utdanning.Status.GYLDIG,
            utdanningslop = listOf("DTDTH1----", "DTGBF3----"),
            nusKoder = listOf("416202", "458333"),
        )

        val bunadstilvirkerfaget = Utdanning(
            id = UUID.fromString("649e9784-0422-4216-9cea-5af581a5c9c4"),
            programomradekode = "DTDTH1----",
            utdanningId = "u_bunadtilvirkerfag",
            navn = "Bunadstilvirkerfaget",
            sluttkompetanse = Utdanning.Sluttkompetanse.SVENNEBREV,
            aktiv = true,
            utdanningstatus = Utdanning.Status.GYLDIG,
            utdanningslop = listOf("DTDTH1----", "DTSTH2----", "DTBUN3----"),
            nusKoder = listOf("416613", "416601"),
        )

        val gjortlerfaget = Utdanning(
            id = UUID.fromString("74db0d0a-549f-4421-b30d-a11a34ede018"),
            programomradekode = "DTDTH1----",
            utdanningId = "u_gjortlerfag",
            navn = "Gjørtlerfaget",
            sluttkompetanse = Utdanning.Sluttkompetanse.SVENNEBREV,
            aktiv = true,
            utdanningstatus = Utdanning.Status.GYLDIG,
            utdanningslop = listOf("DTDTH1----", "DTGTL3----"),
            nusKoder = listOf("458902", "458901"),
        )
    }
}
