package no.nav.mulighetsrommet.api.amo.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.amo.OpplaringKategorisering
import no.nav.mulighetsrommet.api.amo.models.Bransje
import no.nav.mulighetsrommet.api.amo.models.ForerkortKlasse
import no.nav.mulighetsrommet.api.amo.models.InnholdElement
import no.nav.mulighetsrommet.api.amo.models.Kurstype
import no.nav.mulighetsrommet.api.avtale.model.UtdanningslopDto
import no.nav.mulighetsrommet.api.janzz.Sertifisering
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.intellij.lang.annotations.Language
import java.util.UUID
import kotlin.collections.ifEmpty

class OpplaringKategoriseringQueries(private val session: Session) {
    fun getKurstyper(filter: Set<Kurstype.Kode> = emptySet()): Set<Kurstype> {
        @Language("PostgreSQL")
        val query = """
            select id, kode, navn, aktiv
            from opplaring_kategorisering_kurstype
            where (:filter::text[] is null or kode = any(:filter))
            order by navn
        """.trimIndent()
        val params = mapOf(
            "filter" to filter.ifEmpty { null }?.let { koder -> session.createArrayOfValue(koder) { it.name } },
        )

        val kurstyper = session.list(queryOf(query, params)) { it.toKurstype() }
        return kurstyper.toSet()
    }

    fun getForerkortKlasser(): Set<ForerkortKlasse> {
        @Language("PostgreSQL")
        val query = """
            select id, kode, navn
            from opplaring_forerkort
            order by navn
        """.trimIndent()

        val forerkortKlasser = session.list(queryOf(query)) { it.toForerkortKlasse() }
        return forerkortKlasser.toSet()
    }

    fun getBransjer(): Set<Bransje> {
        @Language("PostgreSQL")
        val query = """
            select id, kode, navn
            from opplaring_kategorisering_bransje
            order by navn
        """.trimIndent()

        val bransjer = session.list(queryOf(query)) { it.toBransje() }
        return bransjer.toSet()
    }

    fun getInnholdElementer(): Set<InnholdElement> {
        @Language("PostgreSQL")
        val query = """
            select id, kode, navn
            from opplaring_innhold_element
            order by navn
        """.trimIndent()
        val innholdElementer = session.list(queryOf(query)) { it.toInnholdElement() }
        return innholdElementer.toSet()
    }

    fun get(kategoriseringId: UUID): OpplaringKategorisering? {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_opplaring_kategorisering
            where id = ?
        """.trimIndent()

        return session.single(queryOf(query, kategoriseringId)) { it.toOpplaringKategorisering() }
    }
}

fun Row.toKurstype() = Kurstype(
    id = uuid("id"),
    kode = string("kode").let { Kurstype.Kode.valueOf(it) },
    navn = string("navn"),
)

fun Row.toForerkortKlasse() = ForerkortKlasse(
    id = uuid("id"),
    kode = string("kode").let { ForerkortKlasse.Kode.valueOf(it) },
    navn = string("navn"),
)

fun Row.toBransje() = Bransje(
    id = uuid("id"),
    kode = string("kode").let { Bransje.Kode.valueOf(it) },
    navn = string("navn"),
)

fun Row.toInnholdElement() = InnholdElement(
    id = uuid("id"),
    kode = string("kode").let { InnholdElement.Kode.valueOf(it) },
    navn = string("navn"),
)

fun Row.toOpplaringKategorisering(): OpplaringKategorisering = OpplaringKategorisering(
    kurstype = stringOrNull("kurstype")?.let { JsonIgnoreUnknownKeys.decodeFromString<Kurstype>(it) },
    bransje = stringOrNull("bransje")?.let { JsonIgnoreUnknownKeys.decodeFromString<Bransje>(it) },
    forerkort = string("forerkort").let { JsonIgnoreUnknownKeys.decodeFromString<List<ForerkortKlasse>>(it) }
        .toSet(),
    sertifiseringer = string("sertifiseringer").let { JsonIgnoreUnknownKeys.decodeFromString<List<Sertifisering>>(it) }
        .toSet(),
    innholdElementer = string("innhold_elementer").let {
        JsonIgnoreUnknownKeys.decodeFromString<List<InnholdElement>>(
            it,
        )
    }.toSet(),
    norskprove = boolean("norskprove"),
    utdanningslop = stringOrNull("utdanningslop")?.let { JsonIgnoreUnknownKeys.decodeFromString<UtdanningslopDto>(it) },
)
