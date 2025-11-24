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

class SlateTilPortableTextGjennomforing(
    private val db: ApiDatabase,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    val task: OneTimeTask<Void> = Tasks
        .oneTime(javaClass.simpleName)
        .execute { _, _ -> execute() }

    fun execute() {
        val mapped = getGjennomforingFaneinnhold().map { gjennomforingFaneInnhold ->
            val faneInnhold = gjennomforingFaneInnhold.faneInnhold
            val mappedFaneInnhold = faneInnhold.copy(
                forHvem = faneInnhold.forHvem?.fromSlateFormat(),
                detaljerOgInnhold = faneInnhold.detaljerOgInnhold?.fromSlateFormat(),
                pameldingOgVarighet = faneInnhold.pameldingOgVarighet?.fromSlateFormat(),
                kontaktinfo = faneInnhold.kontaktinfo?.fromSlateFormat(),
                oppskrift = faneInnhold.oppskrift?.fromSlateFormat(),
            )
            return@map GjennomforingFaneInnhold(gjennomforingFaneInnhold.id, mappedFaneInnhold)
        }
        logger.info("Konverterer ${mapped.size} gjennomforinger Faneinnhold fra Slate til Portable Text format")
        db.transaction {
            mapped.forEach { mapped ->
                upsertFaneinnhold(mapped.id, mapped.faneInnhold)
            }
        }
        logger.info("${mapped.size} gjennomforinger konvertert fra Slate til Portable Text format")
    }

    private fun getGjennomforingFaneinnhold(): List<GjennomforingFaneInnhold> = db.session {
        @Language("PostgreSQL")
        val query = """
            select
              id,
              faneinnhold
            from gjennomforing
            where faneinnhold is not null
        """.trimIndent()

        session.list(queryOf(query, emptyMap())) {
            GjennomforingFaneInnhold(
                id = it.uuid("id"),
                faneInnhold = it.string("faneinnhold").let { str -> Json.decodeFromString<Faneinnhold>(str) },
            )
        }
    }

    private fun TransactionalQueryContext.upsertFaneinnhold(id: UUID, faneInnhold: Faneinnhold) {
        @Language("PostgreSQL")
        val query = """
                update gjennomforing
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

data class GjennomforingFaneInnhold(val id: UUID, val faneInnhold: Faneinnhold)
