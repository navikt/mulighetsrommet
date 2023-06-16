package no.nav.mulighetsrommet.api.repositories

import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language

class MetrikkRepository(private val db: Database) {
    fun hentAntallUlesteNotifikasjoner(): Int {
        @Language("PostgreSQL")
        val query = """
            select count(*) as antallUlesteNotifikasjoner from user_notification where done_at is null
        """.trimIndent()

        return queryOf(query).map { it.int("antallUlesteNotifikasjoner") }.asSingle.let { db.run(it) } ?: 0
    }

    fun hentAntallLesteNotifikasjoner(): Int {
        @Language("PostgreSQL")
        val query = """
            select count(*) as antallLesteNotifikasjoner from user_notification where done_at is not null
        """.trimIndent()

        return queryOf(query).map { it.int("antallLesteNotifikasjoner") }.asSingle.let { db.run(it) } ?: 0
    }
}
