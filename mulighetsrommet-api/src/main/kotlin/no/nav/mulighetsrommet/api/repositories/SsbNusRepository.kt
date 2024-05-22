package no.nav.mulighetsrommet.api.repositories

import kotlinx.serialization.Serializable
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.ssb.ClassificationItem
import no.nav.mulighetsrommet.api.clients.ssb.SsbNusData
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.Tiltakskode
import org.intellij.lang.annotations.Language

class SsbNusRepository(private val db: Database) {
    fun upsert(data: SsbNusData, version: String) {
        @Language("PostgreSQL")
        val query = """
        insert into nus_kodeverk(code, name, parent, level, version)
        values(:code, :name, :parent, :level, :version)
        on conflict (code, version) do update set
            name = excluded.name,
            parent = excluded.parent,
            level = excluded.level
        """.trimIndent()

        db.transaction { tx ->
            data.classificationItems.forEach { classificationItem ->
                tx.run(
                    queryOf(
                        query,
                        classificationItem.toSqlParams() + mapOf("version" to version),
                    ).asExecute,
                )
            }
        }
    }

    fun getNusData(tiltakskode: Tiltakskode, version: String): List<NusElement> {
        @Language("PostgreSQL")
        val query = """
        select tiltakskode, tnk.code, tnk.version, name, parent, level
        from tiltakstype_nus_kodeverk tnk
                 join nus_kodeverk nk on tnk.code = nk.code and tnk.version = nk.version
        where tiltakskode = ?::tiltakskode
        and tnk.version = ?
        order by tnk.code
        """.trimIndent()

        return queryOf(query, tiltakskode.name, version)
            .map { it.toNusElement() }
            .asList.let { db.run(it) }
    }
}

private fun ClassificationItem.toSqlParams(): Map<String, String?> {
    return mapOf(
        "code" to code,
        "name" to name,
        "parent" to if (parentCode?.isEmpty() == true) {
            null
        } else {
            parentCode
        },
        "level" to level,
    )
}

private fun Row.toNusElement(): NusElement {
    return NusElement(
        tiltakskode = Tiltakskode.valueOf(string("tiltakskode")),
        code = string("code"),
        name = string("name"),
        parent = stringOrNull("parent"),
        version = string("version"),
        level = string("level"),
    )
}

@Serializable
data class NusElement(
    val tiltakskode: Tiltakskode,
    val code: String,
    val version: String,
    val name: String,
    val parent: String?,
    val level: String,
)
