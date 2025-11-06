package no.nav.mulighetsrommet.api.avtale.task

import arrow.core.getOrNone
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Daily
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.avtale.AvtaleService
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.PortableTextTypedObject
import no.nav.mulighetsrommet.model.Tiltaksadministrasjon
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class SlateTilPortableText(
    private val db: ApiDatabase,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    val task: RecurringTask<Void> = Tasks
        .recurring(javaClass.simpleName, Daily(LocalTime.MIDNIGHT))
        .execute { _, _ ->
            execute(LocalDateTime.now())
        }

    fun execute(now: LocalDateTime) {
        val faneinnholdListe = getAvtalerFaneinnhold()
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
}

data class AvtaleFaneInnhold(val id: UUID, val faneInnhold: Faneinnhold)

fun List<PortableTextTypedObject>.fromSlateFormat(): List<PortableTextTypedObject> {
    if (this.isEmpty()) {
        return emptyList()
    }
    return this.map {
        if (it._key != null) {
            return@map it
        }
        return@map it.toSlateFormat()
    }
}

private fun PortableTextTypedObject.toSlateFormat(): PortableTextTypedObject {
    if (this._type != "block") {
        return this
    }
    val additionalProperties = this.additionalProperties.toMutableMap()
    additionalProperties["style"] = JsonPrimitive("normal")

    if ("listItem" in additionalProperties) {
        additionalProperties["level"] = JsonPrimitive(1)
    }
    val markDefs = additionalProperties.getOrNone("markDefs").getOrNull()?.let {
        if (it is JsonArray && it.isNotEmpty()) {
            return@let it.updateMarkdefs(additionalProperties)
        }
        return@let null
    }
    if (markDefs != null) {
        additionalProperties["markDefs"] = markDefs
    }
    return this.copy(additionalProperties = additionalProperties)
}

private fun JsonArray.updateMarkdefs(additionalProperties: MutableMap<String, JsonElement>): JsonArray {
    val linkMarkDefIndex = this.indexOfFirst { obj ->
        obj is JsonObject && "_type" in obj && obj["_type"].toString() == "link"
    }
    if (linkMarkDefIndex < 0) {
        return this
    }

    val linkMarkDef = this[linkMarkDefIndex].let { it as? JsonObject }
    val linkMarkDefKey = linkMarkDef?.get("_key")?.jsonPrimitive?.content
    val newKey = getOrGenerateKey()

    val children = additionalProperties["children"]?.let { it as? JsonArray }?.map { child ->
        if (child !is JsonObject) {
            return@map child
        }
        val mutChild = child.toMutableMap()
        mutChild["_key"] = JsonPrimitive(getOrGenerateKey(child))

        if ("marks" in mutChild) {
            val childMarks = mutChild["marks"]?.let { it as? JsonArray }?.map { childMark ->
                if (childMark is JsonPrimitive && childMark.toString() == linkMarkDefKey) {
                    return@map JsonPrimitive(newKey)
                }
                return@map childMark
            }
            if (childMarks != null) {
                mutChild["marks"] = JsonArray(childMarks)
            }
        }
        return@map JsonObject(mutChild)
    }
    if (children != null) {
        additionalProperties["children"] = JsonArray(children)
    }
    return this
}

private fun getOrGenerateKey(obj: JsonObject? = null): String {
    if (obj != null && "_key" in obj && obj["_key"] != JsonNull) {
        return obj["_key"].toString()
    }
    return UUID.randomUUID().toString().slice(0..8)
}
