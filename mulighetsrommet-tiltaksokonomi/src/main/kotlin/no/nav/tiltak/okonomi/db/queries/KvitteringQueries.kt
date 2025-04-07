package no.nav.tiltak.okonomi.db.queries

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.withTransaction
import org.intellij.lang.annotations.Language

class KvitteringQueries(private val session: Session) {
    fun insert(kvitteringJson: String) = withTransaction(session) {
        @Language("PostgreSQL")
        val insertBestilling = """
            insert into oebs_kvittering (json) values (:json::jsonb)
        """
        execute(queryOf(insertBestilling, mapOf("json" to kvitteringJson)))
    }
}
