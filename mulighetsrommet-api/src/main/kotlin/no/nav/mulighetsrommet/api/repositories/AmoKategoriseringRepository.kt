package no.nav.mulighetsrommet.api.repositories

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.domain.dto.*
import org.intellij.lang.annotations.Language
import java.util.*

object AmoKategoriseringRepository {
    enum class ForeignIdType {
        AVTALE,
        GJENNOMFORING,
    }

    fun upsert(amoKategorisering: AmoKategorisering, foreignId: UUID, foreignIdType: ForeignIdType, tx: Session) {
        val foreignName = when (foreignIdType) {
            ForeignIdType.AVTALE -> "avtale"
            ForeignIdType.GJENNOMFORING -> "tiltaksgjennomforing"
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
                norskprove = excluded.norskprove,
                forerkort = excluded.forerkort,
                innhold_elementer = excluded.innhold_elementer
        """.trimIndent()

        tx.run(
            queryOf(
                query,
                mutableMapOf("${foreignName}_id" to foreignId).plus(amoKategorisering.toSqlParameters(tx)),
            ).asExecute,
        )

        if (amoKategorisering is AmoKategorisering.BransjeOgYrkesrettet) {
            updateSertifiseringer(foreignId, foreignName, amoKategorisering.sertifiseringer, tx)
        }
    }

    private fun updateSertifiseringer(
        foreignId: UUID,
        foreignName: String,
        sertifiseringer: List<AmoKategorisering.BransjeOgYrkesrettet.Sertifisering>,
        tx: Session,
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

        tx.batchPreparedStatement(
            upsertSertifiseringer,
            sertifiseringer.map { s -> listOf(s.konseptId, s.label) },
        )
        tx.batchPreparedStatement(
            upsertJoinTable,
            sertifiseringer.map { s -> listOf(foreignId, s.konseptId) },
        )
        tx.run(
            queryOf(deleteJoins, foreignId, tx.createArrayOf("bigint", sertifiseringer.map { it.konseptId }))
                .asExecute,
        )
    }

    fun AmoKategorisering.toSqlParameters(tx: Session) =
        when (this) {
            is AmoKategorisering.BransjeOgYrkesrettet -> mapOf(
                "kurstype" to "BRANSJE_OG_YRKESRETTET",
                "bransje" to bransje.name,
                "forerkort" to tx.createArrayOf("forerkort_klasse", this.forerkort.map { it.name }),
                "sertifiseringer" to Json.encodeToString(sertifiseringer),
                "innhold_elementer" to tx.createArrayOf("amo_innhold_element", this.innholdElementer.map { it.name }),
            )
            AmoKategorisering.ForberedendeOpplaeringForVoksne -> mapOf(
                "kurstype" to "FORBEREDENDE_OPPLAERING_FOR_VOKSNE",
            )
            is AmoKategorisering.GrunnleggendeFerdigheter -> mapOf(
                "kurstype" to "GRUNNLEGGENDE_FERDIGHETER",
                "innhold_elementer" to tx.createArrayOf("amo_innhold_element", this.innholdElementer.map { it.name }),
            )
            is AmoKategorisering.Norskopplaering -> mapOf(
                "kurstype" to "NORSKOPPLAERING",
                "norskprove" to norskprove,
                "innhold_elementer" to tx.createArrayOf("amo_innhold_element", this.innholdElementer.map { it.name }),
            )
            AmoKategorisering.Studiespesialisering -> mapOf(
                "kurstype" to "STUDIESPESIALISERING",
            )
        }
}
