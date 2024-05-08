package no.nav.mulighetsrommet.api.repositories

import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.ssb.ClassificationItem
import no.nav.mulighetsrommet.api.clients.ssb.SsbNusData
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language

class SsbNusRepository(private val db: Database) {
    fun saveData(data: SsbNusData, version: String) {
        @Language("PostgreSQL")
        val query = """
        insert into nus_kodeverk(id, name, parent, level, version, self_link)
        values(:id, :name, :parent, :level, :version, :selfLink)
        on conflict (id, version) do update set
            name = excluded.name,
            parent = excluded.parent,
            level = excluded.level,
            self_link = excluded.self_link
        """.trimIndent()

        db.transaction { tx ->
            data.classificationItems.forEach { classificationItem ->
                tx.run(
                    queryOf(
                        query,
                        classificationItem.toSqlParams() + mapOf("version" to version, "selfLink" to data._links.self.href),
                    ).asExecute,
                )
            }
        }
    }
}

private fun ClassificationItem.toSqlParams(): Map<String, String> {
    return mapOf(
        "id" to code,
        "name" to name,
        "parent" to parentCode,
        "level" to level,
    )
}
