package no.nav.mulighetsrommet.api.amo

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import org.intellij.lang.annotations.Language
import java.util.UUID

class OpplaringKategoriseringQueries(private val session: Session) {
    fun getKurstyper(inkluderInaktive: Boolean = false): List<Kurstype> {
        @Language("PostgreSQL")
        val query = """
            select id, kode, navn, aktiv
            from opplaring_kategorisering_kurstype
            where (:alle::bool = true or aktiv = true)
            order by navn
        """.trimIndent()
        val params = mapOf("alle" to inkluderInaktive)

        val kurstyper = session.list(queryOf(query, params)) { it.toKurstype() }
        return kurstyper
    }

    fun getForerkortKlasser(): List<ForerkortKlasse> {
        @Language("PostgreSQL")
        val query = """
            select id, kode, navn
            from opplaring_kategorisering_forerkort
            order by navn
        """.trimIndent()

        val forerkortKlasser = session.list(queryOf(query)) { it.toForerkortKlasse() }
        return forerkortKlasser
    }

    fun getBransjer(): List<Bransje> {
        @Language("PostgreSQL")
        val query = """
            select id, kode, navn
            from opplaring_kategorisering_bransje
            order by navn
        """.trimIndent()

        val bransjer = session.list(queryOf(query)) { it.toBransje() }
        return bransjer
    }
}

data class Kurstype(val id: UUID, val kode: Kode, val navn: String, val aktiv: Boolean) {
    enum class Kode {
        NORSKOPPLARING,
        GRUNNLEGGENDE_FERDIGHETER,
        FORBEREDENDE_OPPLAERING_FOR_VOKSNE,
        BRANSJE_OG_YRKESRETTET,
        STUDIESPESIALISERING,
    }
}

fun Row.toKurstype() = Kurstype(
    id = uuid("id"),
    kode = string("kode").let { Kurstype.Kode.valueOf(it) },
    navn = string("navn"),
    aktiv = boolean("aktiv"),
)

data class ForerkortKlasse(val id: UUID, val kode: Kode, val navn: String) {
    enum class Kode {
        A,
        A1,
        A2,
        AM,
        AM_147,
        B,
        B_78,
        BE,
        C,
        C1,
        C1E,
        CE,
        D,
        D1,
        D1E,
        DE,
        S,
        T,
    }
}

fun Row.toForerkortKlasse() = ForerkortKlasse(
    id = uuid("id"),
    kode = string("kode").let { ForerkortKlasse.Kode.valueOf(it) },
    navn = string("navn"),
)

data class Bransje(val id: UUID, val kode: Kode, val navn: String) {
    enum class Kode {
        INGENIOR_OG_IKT_FAG,
        HELSE_PLEIE_OG_OMSORG,
        BARNE_OG_UNGDOMSARBEID,
        KONTORARBEID,
        BUTIKK_OG_SALGSARBEID,
        BYGG_OG_ANLEGG,
        INDUSTRIARBEID,
        REISELIV_SERVERING_OG_TRANSPORT,
        SERVICEYRKER_OG_ANNET_ARBEID,
        ANDRE_BRANSJER,
    }
}

fun Row.toBransje() = Bransje(
    id = uuid("id"),
    kode = string("kode").let { Bransje.Kode.valueOf(it) },
    navn = string("navn"),
)
