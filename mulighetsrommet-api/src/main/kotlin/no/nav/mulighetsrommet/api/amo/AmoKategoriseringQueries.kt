package no.nav.mulighetsrommet.api.amo

import kotlinx.serialization.json.Json
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.amo.models.ForerkortKlasse
import no.nav.mulighetsrommet.api.amo.models.Kurstype
import no.nav.mulighetsrommet.api.janzz.Sertifisering
import no.nav.mulighetsrommet.database.createBigintArray
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
                innhold_elementer,
                bransje_id,
                kurstype_id,
            ) select
                :${foreignName}_id::uuid,
                :kurstype::amo_kurstype,
                :bransje::amo_bransje,
                :norskprove::boolean,
                :forerkort,
                :innhold_elementer,
                ok_bransje.id,
                ok_kurstype.id,
            from opplaring_kategorisering_bransje ok_bransje
            join opplaring_kategorisering_kurstype ok_kurstype on ok_kurstype.kode = :kurstype
            where ok_bransje.kode = :bransje
            on conflict (${foreignName}_id) do update set
                kurstype = excluded.kurstype,
                bransje = excluded.bransje,
                norskprove = excluded.norskprove,
                forerkort = excluded.forerkort,
                innhold_elementer = excluded.innhold_elementer,
                bransje_id = excluded.bransje_id,
                kurstype_id = excluded.kurstype_id
        """.trimIndent()

        val params = mutableMapOf("${foreignName}_id" to foreignId) + (amoKategorisering.toSqlParameters())

        session.execute(queryOf(query, params))

        if (amoKategorisering.kurstype?.kode == Kurstype.Kode.BRANSJE_OG_YRKESRETTET) {
            updateSertifiseringer(foreignId, foreignName, amoKategorisering.sertifiseringer)
        }
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

    context(session: Session)
    private fun AmoKategorisering.toSqlParameters() = when (kurstype?.kode) {
        Kurstype.Kode.BRANSJE_OG_YRKESRETTET -> mapOf(
            "kurstype" to kurstype.kode.name,
            "bransje" to bransje?.kode?.name,
            "forerkort" to session.createArrayOfForerkortKlasse(forerkort),
            "sertifiseringer" to Json.encodeToString(sertifiseringer),
            "innhold_elementer" to session.createArrayOfAmoInnholdElement(innholdElementer),
        )

        Kurstype.Kode.FORBEREDENDE_OPPLAERING_FOR_VOKSNE -> mapOf(
            "kurstype" to kurstype.kode.name,
            "innhold_elementer" to session.createArrayOfAmoInnholdElement(innholdElementer),
        )

        Kurstype.Kode.GRUNNLEGGENDE_FERDIGHETER -> mapOf(
            "kurstype" to kurstype.kode.name,
            "innhold_elementer" to session.createArrayOfAmoInnholdElement(innholdElementer),
        )

        Kurstype.Kode.NORSKOPPLAERING -> mapOf(
            "kurstype" to kurstype.kode.name,
            "norskprove" to norskprove,
            "innhold_elementer" to session.createArrayOfAmoInnholdElement(innholdElementer),
        )

        Kurstype.Kode.STUDIESPESIALISERING -> mapOf(
            "kurstype" to kurstype.kode.name,
        )

        null -> emptyMap()
    }
}

fun Session.createArrayOfForerkortKlasse(
    items: Collection<ForerkortKlasse>,
): Array = createArrayOf("forerkort_klasse", items.map { it.kode.name })

fun Session.createArrayOfAmoInnholdElement(
    items: Collection<AmoKategorisering.InnholdElement>,
): Array = createArrayOf("amo_innhold_element", items)
