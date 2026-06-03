package no.nav.mulighetsrommet.api.amo

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.amo.db.OpplaringKategoriseringDbo
import no.nav.mulighetsrommet.api.janzz.Sertifisering
import no.nav.mulighetsrommet.database.createBigintArray
import no.nav.mulighetsrommet.database.createUuidArray
import no.nav.mulighetsrommet.utdanning.db.UtdanningslopDbo
import org.intellij.lang.annotations.Language
import java.util.UUID

object AmoKategoriseringQueries {

    context(session: Session)
    fun upsert(
        id: UUID,
        kategorisering: OpplaringKategoriseringDbo?,
    ) {
        if (kategorisering == null) {
            delete(id)
        } else {
            upsert(id, kategorisering)
        }
    }

    context(session: Session)
    private fun upsert(
        kategoriseringId: UUID,
        kategorisering: OpplaringKategoriseringDbo,
    ) {
        @Language("PostgreSQL")
        val query = """
            insert into opplaring_kategorisering (
                id,
                kurstype_id,
                bransje_id,
                utdanningsprogram_id,
                norskprove
            ) select
                :id::uuid,
                :kurstype_id::uuid,
                :bransje_id::uuid,
                :utdanningsprogram_id::uuid,
                :norskprove::boolean
            from opplaring_kategorisering
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

    context(session: Session)
    private fun upsertInnholdsElementer(
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
            where opplaring_kategorisering_id = ? and not (innhold_element_id = any (?))
        """.trimIndent()

        session.batchPreparedStatement(
            upsertJoinTable,
            innholdElementer.map { elementId -> listOf(kategoriseringId, elementId) },
        )

        session.execute(
            queryOf(deleteJoins, kategoriseringId, session.createUuidArray(innholdElementer)),
        )
    }

    context(session: Session)
    private fun updateForerkort(
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

        session.batchPreparedStatement(
            upsertJoinTable,
            forerkort.map { id -> listOf(kategoriseringId, id) },
        )
        session.execute(
            queryOf(deleteJoins, kategoriseringId, session.createUuidArray(forerkort)),
        )
    }

    context(session: Session)
    private fun updateSertifiseringer(
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

        session.batchPreparedStatement(
            upsertSertifiseringer,
            sertifiseringer.map { s -> listOf(s.konseptId, s.label) },
        )
        session.batchPreparedStatement(
            upsertJoinTable,
            sertifiseringer.map { s -> listOf(kategoriseringId, s.konseptId) },
        )
        session.execute(
            queryOf(deleteJoins, kategoriseringId, session.createBigintArray(sertifiseringer.map { it.konseptId })),
        )
    }

    context(session: Session)
    private fun upsertUtdanning(kategoriseringId: UUID, utdanningslopDbo: UtdanningslopDbo?) {
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

        session.batchPreparedStatement(
            upsertJoinTable,
            utdannninger.map { utdanningId -> listOf(kategoriseringId, utdanningId) },
        )

        session.execute(
            queryOf(deleteJoins, kategoriseringId, session.createUuidArray(utdannninger)),
        )
    }

    context(session: Session)
    private fun delete(kategoriseringId: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from opplaring_kategorisering where id = ?::uuid
        """.trimIndent()

        session.update(queryOf(query, kategoriseringId))
    }
}
