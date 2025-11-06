package no.nav.mulighetsrommet.api.avtale.task

import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.json.Json
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.fromSlateFormat
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class SlateTilPortableText(
    private val db: ApiDatabase,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    val task: OneTimeTask<Void> = Tasks
        .oneTime(javaClass.simpleName)
        .execute { _, _ -> execute() }

    fun execute() {
        val mapped = getAvtalerFaneinnhold().map { avtaleFaneInnhold ->
            val faneInnhold = avtaleFaneInnhold.faneInnhold
            val mappedFaneInnhold = faneInnhold.copy(
                forHvem = faneInnhold.forHvem?.fromSlateFormat(),
                detaljerOgInnhold = faneInnhold.detaljerOgInnhold?.fromSlateFormat(),
                pameldingOgVarighet = faneInnhold.pameldingOgVarighet?.fromSlateFormat(),
                kontaktinfo = faneInnhold.kontaktinfo?.fromSlateFormat(),
                oppskrift = faneInnhold.oppskrift?.fromSlateFormat(),
            )
            return@map AvtaleFaneInnhold(avtaleFaneInnhold.id, mappedFaneInnhold)
        }
        logger.info("Konverterer ${mapped.size} avtalers Faneinnhold fra Slate til Portable Text format")
        db.transaction {
            mapped.forEach { mapped ->
                upsertFaneinnhold(mapped.id, mapped.faneInnhold)
            }
        }
        logger.info("${mapped.size} avtaler konvertert fra Slate til Portable Text format")
    }

    private fun getAvtalerFaneinnhold(): List<AvtaleFaneInnhold> = db.session {
        @Language("PostgreSQL")
        val query = """
            select
              id,
              faneinnhold
            from avtale
            where faneinnhold is not null
        """.trimIndent()

        session.list(queryOf(query, emptyMap())) {
            AvtaleFaneInnhold(
                id = it.uuid("id"),
                faneInnhold = it.string("faneinnhold").let { str -> Json.decodeFromString<Faneinnhold>(str) },
            )
        }
    }

    private fun TransactionalQueryContext.upsertFaneinnhold(id: UUID, faneInnhold: Faneinnhold) {
        @Language("PostgreSQL")
        val query = """
                update avtale
                set
                    faneinnhold = :faneinnhold::jsonb
                 where id = :id::uuid
        """.trimIndent()

        val params = mapOf(
            "id" to id.toString(),
            "faneinnhold" to Json.encodeToString(faneInnhold),
        )

        session.execute(queryOf(query, params))
    }
}

data class AvtaleFaneInnhold(val id: UUID, val faneInnhold: Faneinnhold)
