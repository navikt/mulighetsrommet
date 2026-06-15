package no.nav.mulighetsrommet.api.fixtures

import kotliquery.Query
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.amo.models.InnholdElement
import org.intellij.lang.annotations.Language
import java.util.UUID

object InnholdElementFixtures {
    val grunnleggendeFerdigheter =
        InnholdElement(
            id = UUID.fromString("e8b6c5d8-26a2-4aed-8496-db57970e9736"),
            kode = InnholdElement.Kode.GRUNNLEGGENDE_FERDIGHETER,
            navn = "Grunnleggende ferdigheter",
        )
    val teoretiskOpplaring =
        InnholdElement(
            id = UUID.fromString("a19cfe15-7aca-4f6d-bf09-353f9af55f41"),
            kode = InnholdElement.Kode.TEORETISK_OPPLAERING,
            navn = "Teoretisk opplæring",
        )
    val jobbsokerKompetanse =
        InnholdElement(
            id = UUID.fromString("7d7677bf-a08a-4798-976b-7b0c82b5771f"),
            kode = InnholdElement.Kode.JOBBSOKER_KOMPETANSE,
            navn = "Jobbsøkerkompetanse",
        )
    val praksis =
        InnholdElement(
            id = UUID.fromString("5c3248e3-03fe-429b-b03f-fd0ee8d942eb"),
            kode = InnholdElement.Kode.PRAKSIS,
            navn = "Praksis",
        )
    val arbeidsmarkedskunnskap =
        InnholdElement(
            id = UUID.fromString("a217c04d-88fe-46f1-9c46-8057a61699bf"),
            kode = InnholdElement.Kode.ARBEIDSMARKEDSKUNNSKAP,
            navn = "Arbeidsmarkedskunnskap",
        )
    val norskopplaring =
        InnholdElement(
            id = UUID.fromString("0f889755-1c3d-4e64-84fb-45c602d88a7c"),
            kode = InnholdElement.Kode.NORSKOPPLAERING,
            navn = "Norskopplæring",
        )
    val bransjerettetOpplaring =
        InnholdElement(
            id = UUID.fromString("9b52c681-6188-441c-b6bb-18f874386188"),
            kode = InnholdElement.Kode.BRANSJERETTET_OPPLARING,
            navn = "Bransjerettet opplæring",
        )
    fun all() = setOf(
        grunnleggendeFerdigheter,
        teoretiskOpplaring,
        jobbsokerKompetanse,
        praksis,
        arbeidsmarkedskunnskap,
        norskopplaring,
        bransjerettetOpplaring,
    )

    fun query(): Query {
        val inserts = all().joinToString(",\n") { "('${it.id}','${it.kode.name}', '${it.navn}')" }

        @Language("PostgreSQL")
        val query = """
        insert into public.opplaring_innhold_element (id, kode, navn)
            values $inserts
       on conflict (kode) do update
       set id = excluded.id,
           navn = excluded.navn;
        """
        return queryOf(query)
    }
}
