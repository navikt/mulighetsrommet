package no.nav.mulighetsrommet.api.amo

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.amo.db.OpplaringKategoriseringDbo
import no.nav.mulighetsrommet.api.janzz.Sertifisering
import no.nav.mulighetsrommet.database.createBigintArray
import no.nav.mulighetsrommet.database.createUuidArray
import org.intellij.lang.annotations.Language
import java.sql.Array
import java.util.UUID

object AmoKategoriseringQueries {

    enum class Relation {
        AVTALE,
        GJENNOMFORING,
    }

    context(session: Session)
    fun upsert(
        relation: Relation,
        id: UUID,
        kategorisering: OpplaringKategoriseringDbo?,
    ) {
        val foreignName = when (relation) {
            Relation.AVTALE -> "avtale"
            Relation.GJENNOMFORING -> "gjennomforing"
        }
        if (kategorisering == null) {
            delete(foreignName, id)
        } else {
            upsert(foreignName, id, kategorisering)
        }
    }

    context(session: Session)
    private fun upsert(
        foreignName: String,
        foreignId: UUID,
        kategorisering: OpplaringKategoriseringDbo,
    ) {
        @Language("PostgreSQL")
        val query = """
            insert into ${foreignName}_amo_kategorisering (
                ${foreignName}_id,
                kurstype,
                bransje,
                norskprove,
                innhold_elementer,
                bransje_id,
                kurstype_id
            ) select
                :${foreignName}_id::uuid,
                ok_kurstype.kode::amo_kurstype,
                ok_bransje.kode::amo_bransje,
                :norskprove::boolean,
                :innhold_elementer,
                :bransje_id,
                :kurstype_id
            from opplaring_kategorisering_kurstype ok_kurstype
            left join opplaring_kategorisering_bransje ok_bransje on ok_bransje.id = :bransje_id
            where ok_kurstype.id = :kurstype_id
            on conflict (${foreignName}_id) do update set
                kurstype = excluded.kurstype,
                bransje = excluded.bransje,
                norskprove = excluded.norskprove,
                innhold_elementer = excluded.innhold_elementer,
                bransje_id = excluded.bransje_id,
                kurstype_id = excluded.kurstype_id
        """.trimIndent()

        val params = mutableMapOf("${foreignName}_id" to foreignId) + mapOf(
            "kurstype_id" to kategorisering.kurstypeId,
            "bransje_id" to kategorisering.bransjeId,
            "innhold_elementer" to session.createArrayOfInnholdElement(kategorisering.innholdElementer),
            "norskprove" to kategorisering.norskprove,
        )

        session.execute(queryOf(query, params))

        updateSertifiseringer(foreignId, foreignName, kategorisering.sertifiseringer)
        updateForerkort(foreignId, foreignName, kategorisering.forerkort)
    }

    context(session: Session)
    private fun updateForerkort(
        foreignId: UUID,
        foreignName: String,
        forerkort: Set<UUID>,
    ) {
        @Language("PostgreSQL")
        val upsertJoinTable = """
        insert into ${foreignName}_amo_kategorisering_forerkort(
            ${foreignName}_id,
            forerkort_id
        )
        values (?, ?)
        on conflict do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteJoins = """
            delete from ${foreignName}_amo_kategorisering_forerkort
            where ${foreignName}_id = ? and not (forerkort_id = any (?))
        """.trimIndent()

        session.batchPreparedStatement(
            upsertJoinTable,
            forerkort.map { id -> listOf(foreignId, id) },
        )
        session.execute(
            queryOf(deleteJoins, foreignId, session.createUuidArray(forerkort)),
        )
    }

    context(session: Session)
    private fun updateSertifiseringer(
        foreignId: UUID,
        foreignName: String,
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
        insert into ${foreignName}_amo_kategorisering_sertifisering (
            ${foreignName}_id,
            konsept_id
        )
        values (?, ?)
        on conflict do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteJoins = """
            delete from ${foreignName}_amo_kategorisering_sertifisering
            where ${foreignName}_id = ? and not (konsept_id = any (?))
        """.trimIndent()

        session.batchPreparedStatement(
            upsertSertifiseringer,
            sertifiseringer.map { s -> listOf(s.konseptId, s.label) },
        )
        session.batchPreparedStatement(
            upsertJoinTable,
            sertifiseringer.map { s -> listOf(foreignId, s.konseptId) },
        )
        session.execute(
            queryOf(deleteJoins, foreignId, session.createBigintArray(sertifiseringer.map { it.konseptId })),
        )
    }

    context(session: Session)
    private fun delete(foreignName: String, foreignId: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from ${foreignName}_amo_kategorisering where ${foreignName}_id = ?::uuid
        """.trimIndent()

        session.update(queryOf(query, foreignId))

        updateSertifiseringer(foreignId, foreignName, emptySet())
    }
}

fun Session.createArrayOfInnholdElement(
    items: Collection<OpplaringKategorisering.InnholdElement>,
): Array = createArrayOf("amo_innhold_element", items)
