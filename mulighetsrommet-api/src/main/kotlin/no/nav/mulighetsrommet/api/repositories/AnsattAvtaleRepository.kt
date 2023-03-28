package no.nav.mulighetsrommet.api.repositories

import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.query
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class AnsattAvtaleRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun lagreAnsvarlig(avtaleId: UUID, navIdent: String) = query {
        logger.info("Lagrer avtale id=$avtaleId til ansatt")

        @Language("PostgreSQL")
        val query = """
            insert into ansatt_avtale (navident, avtale_id)
            values (?, ?::uuid)
        """.trimIndent()

        queryOf(query, navIdent, avtaleId).asExecute.let { db.run(it) }
    }
}
