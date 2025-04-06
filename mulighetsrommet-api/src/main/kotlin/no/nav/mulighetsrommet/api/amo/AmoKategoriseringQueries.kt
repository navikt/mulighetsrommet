package no.nav.mulighetsrommet.api.amo

import kotlinx.serialization.json.Json
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.database.createBigintArray
import no.nav.mulighetsrommet.model.AmoKategorisering
import org.intellij.lang.annotations.Language
import java.sql.Array
import java.util.*

class AmoKategoriseringQueries(private val session: Session) {

    fun upsert(dbo: GjennomforingDbo) {
        if (dbo.amoKategorisering == null) {
            delete(dbo.id, ForeignIdType.GJENNOMFORING)
        } else {
            upsert(dbo.amoKategorisering, dbo.id, ForeignIdType.GJENNOMFORING)
        }
    }

    fun upsert(dbo: AvtaleDbo) {
        if (dbo.amoKategorisering == null) {
            delete(dbo.id, ForeignIdType.AVTALE)
        } else {
            upsert(dbo.amoKategorisering, dbo.id, ForeignIdType.AVTALE)
        }
    }

    private enum class ForeignIdType {
        AVTALE,
        GJENNOMFORING,
    }

    private fun upsert(amoKategorisering: AmoKategorisering, foreignId: UUID, foreignIdType: ForeignIdType) = with(session) {
        val foreignName = when (foreignIdType) {
            ForeignIdType.AVTALE -> "avtale"
            ForeignIdType.GJENNOMFORING -> "gjennomforing"
        }

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

        execute(queryOf(query, params))

        if (amoKategorisering is AmoKategorisering.BransjeOgYrkesrettet) {
            updateSertifiseringer(foreignId, foreignName, amoKategorisering.sertifiseringer)
        }
    }

    private fun updateSertifiseringer(
        foreignId: UUID,
        foreignName: String,
        sertifiseringer: List<AmoKategorisering.BransjeOgYrkesrettet.Sertifisering>,
    ) = with(session) {
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

        batchPreparedStatement(
            upsertSertifiseringer,
            sertifiseringer.map { s -> listOf(s.konseptId, s.label) },
        )
        batchPreparedStatement(
            upsertJoinTable,
            sertifiseringer.map { s -> listOf(foreignId, s.konseptId) },
        )
        execute(
            queryOf(deleteJoins, foreignId, createBigintArray(sertifiseringer.map { it.konseptId })),
        )
    }

    private fun delete(foreignId: UUID, foreignIdType: ForeignIdType) = with(session) {
        val foreignName = when (foreignIdType) {
            ForeignIdType.AVTALE -> "avtale"
            ForeignIdType.GJENNOMFORING -> "gjennomforing"
        }

        @Language("PostgreSQL")
        val query = """
            delete from ${foreignName}_amo_kategorisering where ${foreignName}_id = ?::uuid
        """.trimIndent()

        update(queryOf(query, foreignId))

        updateSertifiseringer(foreignId, foreignName, emptyList())
    }

    private fun AmoKategorisering.toSqlParameters() = when (this) {
        is AmoKategorisering.BransjeOgYrkesrettet -> mapOf(
            "kurstype" to "BRANSJE_OG_YRKESRETTET",
            "bransje" to bransje.name,
            "forerkort" to session.createArrayOfForerkortKlasse(forerkort),
            "sertifiseringer" to Json.encodeToString(sertifiseringer),
            "innhold_elementer" to session.createArrayOfAmoInnholdElement(innholdElementer),
        )

        AmoKategorisering.ForberedendeOpplaeringForVoksne -> mapOf(
            "kurstype" to "FORBEREDENDE_OPPLAERING_FOR_VOKSNE",
        )

        is AmoKategorisering.GrunnleggendeFerdigheter -> mapOf(
            "kurstype" to "GRUNNLEGGENDE_FERDIGHETER",
            "innhold_elementer" to session.createArrayOfAmoInnholdElement(innholdElementer),
        )

        is AmoKategorisering.Norskopplaering -> mapOf(
            "kurstype" to "NORSKOPPLAERING",
            "norskprove" to norskprove,
            "innhold_elementer" to session.createArrayOfAmoInnholdElement(innholdElementer),
        )

        AmoKategorisering.Studiespesialisering -> mapOf(
            "kurstype" to "STUDIESPESIALISERING",
        )
    }
}

fun Session.createArrayOfForerkortKlasse(
    items: List<AmoKategorisering.BransjeOgYrkesrettet.ForerkortKlasse>,
): Array = createArrayOf("forerkort_klasse", items)

fun Session.createArrayOfAmoInnholdElement(
    items: List<AmoKategorisering.InnholdElement>,
): Array = createArrayOf("amo_innhold_element", items)
