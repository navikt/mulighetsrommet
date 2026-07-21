package no.nav.mulighetsrommet.api.fixtures

import kotliquery.Query
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.opplaring.Bransje
import org.intellij.lang.annotations.Language
import java.util.UUID

object BransjeFixtures {
    val byggOgAnlegg = Bransje(
        id = UUID.fromString("d9b1c8e0-1c3a-4f5b-9c2e-1a2b3c4d5e6f"),
        kode = Bransje.Kode.BYGG_OG_ANLEGG,
        navn = "Bygg og anlegg",
    )
    val ingenior = Bransje(
        id = UUID.fromString("d04dff0d-fdca-4839-9bdc-44c722af5d6f"),
        kode = Bransje.Kode.INGENIOR_OG_IKT_FAG,
        navn = "Ingeniør- og IKT-fag",
    )
    val helseOgPleier = Bransje(
        id = UUID.fromString("82bd7ce0-70f1-448b-8773-9015dea613e7"),
        kode = Bransje.Kode.HELSE_PLEIE_OG_OMSORG,
        navn = "Helse, pleie og omsorg",
    )
    val barnOgUngdomsarbeid = Bransje(
        id = UUID.fromString("14886bad-a495-420a-9bae-d33e2d88041a"),
        kode = Bransje.Kode.BARNE_OG_UNGDOMSARBEID,
        navn = "Barne- og ungdomsarbeid",
    )
    val kontorarbeid = Bransje(
        id = UUID.fromString("a86c1f7a-47c3-4f69-b138-89341107e0eb"),
        kode = Bransje.Kode.KONTORARBEID,
        navn = "Kontorarbeid",
    )
    val butikk = Bransje(
        id = UUID.fromString("e6749d6c-aacf-452d-baf2-d5fb5021912b"),
        kode = Bransje.Kode.BUTIKK_OG_SALGSARBEID,
        navn = "Butikk- og salgsarbeid",
    )
    val industriarbeid = Bransje(
        id = UUID.fromString("4733d7ef-d106-47a4-b335-bfd132c8ad31"),
        kode = Bransje.Kode.INDUSTRIARBEID,
        navn = "Industriarbeid",
    )
    val reiseliv = Bransje(
        id = UUID.fromString("c8851a31-6362-4ee2-8989-e5da95726076"),
        kode = Bransje.Kode.REISELIV_SERVERING_OG_TRANSPORT,
        navn = "Reiseliv, servering og transport",
    )
    val serviceyrker = Bransje(
        id = UUID.fromString("47c9d5f0-66ea-4e68-949d-86733346ee80"),
        kode = Bransje.Kode.SERVICEYRKER_OG_ANNET_ARBEID,
        navn = "Serviceyrker og annet arbeid",
    )
    val andre = Bransje(
        id = UUID.fromString("54ccb278-92ea-4835-8566-659e98602905"),
        kode = Bransje.Kode.ANDRE_BRANSJER,
        navn = "Andre bransjer",
    )

    fun all() = setOf(
        byggOgAnlegg,
        ingenior,
        helseOgPleier,
        barnOgUngdomsarbeid,
        kontorarbeid,
        butikk,
        industriarbeid,
        reiseliv,
        serviceyrker,
        andre,
    )

    fun query(): Query {
        val inserts = all().joinToString(",\n") { "('${it.id}','${it.kode.name}', '${it.navn}')" }

        @Language("PostgreSQL")
        val query = """
            insert into public.opplaring_kategorisering_bransje (id, kode, navn)
            values $inserts
       on conflict (id) do nothing;
    """
        return queryOf(query)
    }
}
