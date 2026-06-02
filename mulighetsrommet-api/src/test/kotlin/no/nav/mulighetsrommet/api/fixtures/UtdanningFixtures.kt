package no.nav.mulighetsrommet.api.fixtures

import kotliquery.Query
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.avtale.model.UtdanningslopDto
import org.intellij.lang.annotations.Language
import java.util.UUID

object UtdanningFixtures {
    object UtdanningsProgram {
        val byggOgAnlegg = UtdanningslopDto.Utdanningsprogram(
            id = UUID.fromString("1390a963-e9b2-4677-bb87-243f4638b7a1"),
            navn = "Bygg- og anleggsteknikk",
        )
        val handVerkDesignOgProduktutvikling = UtdanningslopDto.Utdanningsprogram(
            id = UUID.fromString("1626096d-f1ac-4c34-aa93-741503bc5584"),
            navn = "Håndverk, design og produktutvikling",
        )

        @Language("PostgreSQL")
        private val UTDANNINGSPROGRAM_SQL = """
            insert into public.utdanningsprogram (id, navn, nus_koder, programomradekode, utdanningsprogram_type)
            values  ('${byggOgAnlegg.id}', '${byggOgAnlegg.navn}', '{3571}', 'BABAT1----', 'YRKESFAGLIG'),
                    ('${handVerkDesignOgProduktutvikling.id}','${handVerkDesignOgProduktutvikling.navn}', '{3169}', 'DTDTH1----', 'YRKESFAGLIG')
            on conflict (id) do nothing;
"""
        fun query(): Query = queryOf(UTDANNINGSPROGRAM_SQL)
    }

    object Utdanninger {
        val fjellOgBergverksfaget = UtdanningslopDto.Utdanning(
            id = UUID.fromString("c8ac69fa-6ba6-494f-95e1-6ec9be06086d"),
            navn = "Fjell- og bergverksfaget",
        )
        val banemontorfaget = UtdanningslopDto.Utdanning(
            id = UUID.fromString("d02ffbea-7f0e-42ff-91a0-88d56277699d"),
            navn = "Banemontørfaget",
        )
        val byggdrifterfaget = UtdanningslopDto.Utdanning(
            id = UUID.fromString("32fff4d1-ef8f-4990-8184-7a7b34febe3b"),
            navn = "Byggdrifterfaget",
        )

        val glassblaserfaget = UtdanningslopDto.Utdanning(
            id = UUID.fromString("7a7a7c50-4800-4e97-be97-f4a87216c8f5"),
            navn = "Glassblåserfaget",
        )
        val bunadstilvirkerfaget = UtdanningslopDto.Utdanning(
            id = UUID.fromString("649e9784-0422-4216-9cea-5af581a5c9c4"),
            navn = "Bunadstilvirkerfaget",
        )
        val gjortlerfaget = UtdanningslopDto.Utdanning(
            id = UUID.fromString("74db0d0a-549f-4421-b30d-a11a34ede018"),
            navn = "Gjørtlerfaget",
        )

        @Language("PostgreSQL")
        private val UTDANNING_SQL = """
                insert into public.utdanning (id, utdanning_id, programomradekode, navn, sluttkompetanse, aktiv, utdanningstatus, utdanningslop, programlop_start, nus_koder)
                values  ('${fjellOgBergverksfaget.id}', 'u_fjell_bergverksfag', 'BAFJE3----', '${fjellOgBergverksfaget.navn}', 'FAGBREV', true, 'GYLDIG', '{BABAT1----,BAANL2----,BAFJE3----}', '${UtdanningsProgram.byggOgAnlegg.id}', '{458409}'),
                        ('${banemontorfaget.id}', 'u_banemontorfag', 'BABAN3----', '${banemontorfaget.navn}', 'FAGBREV', true, 'GYLDIG', '{BABAT1----,BAANL2----,BABAN3----}', '${UtdanningsProgram.byggOgAnlegg.id}', '{457103}'),
                        ('${byggdrifterfaget.id}', 'u_byggdrifterfag', 'BABDR3----', '${byggdrifterfaget.navn}', 'FAGBREV', true, 'GYLDIG', '{BABAT1----,BABDR3----}','${UtdanningsProgram.byggOgAnlegg.id}', '{457136}'),
                        ('${glassblaserfaget.id}', 'u_glasshandverkerfag', 'DTGBF3----','${glassblaserfaget.navn}', 'FAGBREV', true, 'GYLDIG', '{DTDTH1----,DTGBF3----}', '${UtdanningsProgram.handVerkDesignOgProduktutvikling.id}', '{416202,458333}'),
                        ('${bunadstilvirkerfaget.id}', 'u_bunadtilvirkerfag', 'DTBUN3----', '${bunadstilvirkerfaget.navn}', 'SVENNEBREV', true, 'GYLDIG', '{DTDTH1----,DTSTH2----,DTBUN3----}','${UtdanningsProgram.handVerkDesignOgProduktutvikling.id}', '{416613,416601}'),
                        ('${gjortlerfaget.id}', 'u_gjortlerfag', 'DTGTL3----', '${gjortlerfaget.navn}', 'SVENNEBREV', true, 'GYLDIG', '{DTDTH1----,DTGTL3----}', '${UtdanningsProgram.handVerkDesignOgProduktutvikling.id}', '{458902,458901}')
                on conflict (id) do nothing;
                """
        fun query(): Query = queryOf(UTDANNING_SQL)
    }
}
