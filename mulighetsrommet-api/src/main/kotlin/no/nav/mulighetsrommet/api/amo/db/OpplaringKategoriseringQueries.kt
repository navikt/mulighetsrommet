package no.nav.mulighetsrommet.api.amo.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.amo.OpplaringKategorisering
import no.nav.mulighetsrommet.api.amo.models.Bransje
import no.nav.mulighetsrommet.api.amo.models.ForerkortKlasse
import no.nav.mulighetsrommet.api.amo.models.InnholdElement
import no.nav.mulighetsrommet.api.amo.models.Kurstype
import no.nav.mulighetsrommet.api.avtale.model.UtdanningslopDto
import no.nav.mulighetsrommet.api.janzz.Sertifisering
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.createBigintArray
import no.nav.mulighetsrommet.database.createUuidArray
import no.nav.mulighetsrommet.database.withTransaction
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import org.intellij.lang.annotations.Language
import java.util.UUID
import kotlin.collections.ifEmpty

object OpplaringKategoriseringQueries {
    context(session: TransactionalSession)
    fun upsert(
        kategoriseringId: UUID,
        kategorisering: OpplaringKategoriseringDbo?,
    ): Unit = withTransaction(session) {
        if (kategorisering == null) {
            delete(kategoriseringId)
            return
        }

        @Language("PostgreSQL")
        val query = """
            insert into opplaring_kategorisering (
                id,
                kurstype_id,
                bransje_id,
                utdanningsprogram_id,
                norskprove
            ) values (
                :id::uuid,
                :kurstype_id::uuid,
                :bransje_id::uuid,
                :utdanningsprogram_id::uuid,
                :norskprove::boolean
            )
            on conflict (id) do update set
                kurstype_id = excluded.kurstype_id,
                bransje_id = excluded.bransje_id,
                utdanningsprogram_id = excluded.utdanningsprogram_id,
                norskprove = excluded.norskprove
        """.trimIndent()

        val params = mapOf(
            "id" to kategoriseringId,
            "kurstype_id" to kategorisering.kurstypeId,
            "bransje_id" to kategorisering.bransjeId,
            "norskprove" to kategorisering.norskprove,
            "utdanningsprogram_id" to kategorisering.utdanningslop?.utdanningsprogram,
        )

        session.execute(queryOf(query, params))

        updateSertifiseringer(kategoriseringId, kategorisering.sertifiseringer)
        updateForerkort(kategoriseringId, kategorisering.forerkort)
        upsertUtdanning(kategoriseringId, kategorisering.utdanningslop)
        upsertInnholdsElementer(kategoriseringId, kategorisering.innholdElementer)
    }

    private fun TransactionalSession.upsertInnholdsElementer(
        kategoriseringId: UUID,
        innholdElementer: Set<UUID>,
    ) {
        @Language("PostgreSQL")
        val upsertJoinTable = """
        insert into opplaring_kategorisering_innhold_element (
            opplaring_kategorisering_id,
            innhold_element_id
        )
        values (?, ?)
        on conflict do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteJoins = """
            delete from opplaring_kategorisering_innhold_element
            where opplaring_kategorisering_id = ?
        """.trimIndent()

        execute(
            queryOf(deleteJoins, kategoriseringId),
        )

        batchPreparedStatement(
            upsertJoinTable,
            innholdElementer.map { elementId -> listOf(kategoriseringId, elementId) },
        )
    }

    private fun TransactionalSession.updateForerkort(
        kategoriseringId: UUID,
        forerkort: Set<UUID>,
    ) {
        @Language("PostgreSQL")
        val upsertJoinTable = """
        insert into opplaring_kategorisering_forerkort(
            opplaring_kategorisering_id,
            forerkort_id
        )
        values (?, ?)
        on conflict do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteJoins = """
            delete from opplaring_kategorisering_forerkort
            where opplaring_kategorisering_id = ? and not (forerkort_id = any (?))
        """.trimIndent()

        batchPreparedStatement(
            upsertJoinTable,
            forerkort.map { id -> listOf(kategoriseringId, id) },
        )
        execute(
            queryOf(deleteJoins, kategoriseringId, createUuidArray(forerkort)),
        )
    }

    private fun TransactionalSession.updateSertifiseringer(
        kategoriseringId: UUID,
        sertifiseringer: Set<Sertifisering>,
    ) {
        @Language("PostgreSQL")
        val upsertSertifiseringer = """
        insert into amo_sertifisering (
            konsept_id,
            label
        )
        values (?, ?)
        on conflict (konsept_id) do update set label = excluded.label
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertJoinTable = """
        insert into opplaring_kategorisering_sertifisering (
            opplaring_kategorisering_id,
            konsept_id
        )
        values (?, ?)
        on conflict do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteJoins = """
            delete from opplaring_kategorisering_sertifisering
            where opplaring_kategorisering_id = ? and not (konsept_id = any (?))
        """.trimIndent()

        batchPreparedStatement(
            upsertSertifiseringer,
            sertifiseringer.map { s -> listOf(s.konseptId, s.label) },
        )
        batchPreparedStatement(
            upsertJoinTable,
            sertifiseringer.map { s -> listOf(kategoriseringId, s.konseptId) },
        )
        execute(
            queryOf(deleteJoins, kategoriseringId, createBigintArray(sertifiseringer.map { it.konseptId })),
        )
    }

    private fun TransactionalSession.upsertUtdanning(kategoriseringId: UUID, utdanningslopDbo: UtdanningslopDbo?) {
        @Language("PostgreSQL")
        val upsertJoinTable = """
        insert into opplaring_kategorisering_utdanning (
            opplaring_kategorisering_id,
            utdanning_id
        )
        values (?, ?)
        on conflict do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteJoins = """
            delete from opplaring_kategorisering_utdanning
            where opplaring_kategorisering_id = ? and not (utdanning_id = any (?))
        """.trimIndent()

        val utdannninger = utdanningslopDbo?.utdanninger ?: emptyList()

        batchPreparedStatement(
            upsertJoinTable,
            utdannninger.map { utdanningId -> listOf(kategoriseringId, utdanningId) },
        )

        execute(
            queryOf(deleteJoins, kategoriseringId, createUuidArray(utdannninger)),
        )
    }

    private fun TransactionalSession.delete(kategoriseringId: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from opplaring_kategorisering where id = ?::uuid
        """.trimIndent()

        update(queryOf(query, kategoriseringId))
    }

    context(session: Session)
    fun get(kategoriseringId: UUID): OpplaringKategorisering? {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_opplaring_kategorisering
            where id = ?
        """.trimIndent()

        return session.single(queryOf(query, kategoriseringId)) { it.toOpplaringKategorisering() }
    }

    context(session: Session)
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

    context(session: Session)
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

    context(session: Session)
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

    context(session: Session)
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
