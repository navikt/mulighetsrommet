package no.nav.tiltak.historikk

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlinx.serialization.json.Json
import java.util.UUID

private val jsonPrettyPrint = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

internal fun String.prettyPrintJson(): String = jsonPrettyPrint.encodeToString(Json.parseToJsonElement(this))

internal infix fun Collection<TiltakshistorikkV1Dto>.shouldMatchExactlyIds(expected: Collection<UUID>) {
    map { it.id } shouldContainExactlyInAnyOrder expected
}
