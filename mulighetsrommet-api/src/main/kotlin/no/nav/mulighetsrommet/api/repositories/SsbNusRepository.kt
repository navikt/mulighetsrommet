package no.nav.mulighetsrommet.api.repositories

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.ssb.ClassificationItem
import no.nav.mulighetsrommet.api.clients.ssb.SsbNusData
import no.nav.mulighetsrommet.database.Database
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
}

private fun ClassificationItem.toSqlParams(): Map<String, String> {
    return mapOf(
        "code" to code,
        "name" to name,
        "parent" to parentCode,
        "level" to level,
    )
}
