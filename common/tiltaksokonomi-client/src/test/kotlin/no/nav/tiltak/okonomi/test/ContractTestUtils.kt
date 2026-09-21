package no.nav.tiltak.okonomi.test

import com.diffplug.selfie.coroutines.expectSelfie
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

val prettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

suspend inline fun <reified T> assertContract(expected: T) {
    val json = prettyJson.encodeToString(expected)

    expectSelfie(json).toMatchDisk()

    Json.decodeFromString<T>(json) shouldBe expected
}
