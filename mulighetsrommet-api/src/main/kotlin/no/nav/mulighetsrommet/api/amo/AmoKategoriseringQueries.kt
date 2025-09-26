package no.nav.mulighetsrommet.api.amo

import kotlinx.serialization.json.Json
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.createBigintArray
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.AmoKurstype
import org.intellij.lang.annotations.Language
import java.sql.Array
import java.util.*

object AmoKategoriseringQueries {

    enum class Relation {
        AVTALE,
        GJENNOMFORING,
    }

    context(session: Session)
    fun upsert(
        relation: Relation,
        id: UUID,
        kategorisering: AmoKategorisering?,
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
        amoKategorisering: AmoKategorisering,
    ) {
        @Language("PostgreSQL")
        val query = """
            insert into ${foreignName}_amo_kategorisering (
                ${foreignName}_id,
                kurstype,
                bransje,
                norskprove,
                forerkort,
                innhold_elementer
            ) values (
                :${foreignName}_id::uuid,
                :kurstype::amo_kurstype,
                :bransje::amo_bransje,
                :norskprove::boolean,
                :forerkort,
                :innhold_elementer
            ) on conflict (${foreignName}_id) do update set
                kurstype = excluded.kurstype,
                bransje = excluded.bransje,
                norskprove = excluded.norskprove,
                forerkort = excluded.forerkort,
                innhold_elementer = excluded.innhold_elementer
        """.trimIndent()

        val params = mutableMapOf("${foreignName}_id" to foreignId) + (amoKategorisering.toSqlParameters())

        session.execute(queryOf(query, params))

        if (amoKategorisering is AmoKategorisering.BransjeOgYrkesrettet) {
            updateSertifiseringer(foreignId, foreignName, amoKategorisering.sertifiseringer)
        }
    }

    context(session: Session)
    private fun updateSertifiseringer(
        foreignId: UUID,
        foreignName: String,
        sertifiseringer: List<AmoKategorisering.BransjeOgYrkesrettet.Sertifisering>,
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

        updateSertifiseringer(foreignId, foreignName, emptyList())
    }

    context(session: Session)
    private fun AmoKategorisering.toSqlParameters() = when (this) {
        is AmoKategorisering.BransjeOgYrkesrettet -> mapOf(
            "kurstype" to AmoKurstype.BRANSJE_OG_YRKESRETTET.name,
            "bransje" to bransje.name,
            "forerkort" to session.createArrayOfForerkortKlasse(forerkort),
            "sertifiseringer" to Json.encodeToString(sertifiseringer),
            "innhold_elementer" to session.createArrayOfAmoInnholdElement(innholdElementer),
        )

        AmoKategorisering.ForberedendeOpplaeringForVoksne -> mapOf(
            "kurstype" to AmoKurstype.FORBEREDENDE_OPPLAERING_FOR_VOKSNE.name,
        )

        is AmoKategorisering.GrunnleggendeFerdigheter -> mapOf(
            "kurstype" to AmoKurstype.GRUNNLEGGENDE_FERDIGHETER.name,
            "innhold_elementer" to session.createArrayOfAmoInnholdElement(innholdElementer),
        )

        is AmoKategorisering.Norskopplaering -> mapOf(
            "kurstype" to AmoKurstype.NORSKOPPLAERING.name,
            "norskprove" to norskprove,
            "innhold_elementer" to session.createArrayOfAmoInnholdElement(innholdElementer),
        )

        AmoKategorisering.Studiespesialisering -> mapOf(
            "kurstype" to AmoKurstype.STUDIESPESIALISERING.name,
        )
    }
}

fun Session.createArrayOfForerkortKlasse(
    items: List<AmoKategorisering.BransjeOgYrkesrettet.ForerkortKlasse>,
): Array = createArrayOf("forerkort_klasse", items)

fun Session.createArrayOfAmoInnholdElement(
    items: List<AmoKategorisering.InnholdElement>,
): Array = createArrayOf("amo_innhold_element", items)
