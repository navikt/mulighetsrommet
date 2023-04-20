package no.nav.mulighetsrommet.arena.adapter.events

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ArenaJsonElementDeserializerTest : FunSpec({

    test("should replace all occurrences of \\u0000 with empty string") {
        val d = ArenaJsonElementDeserializer()
        val result = d.deserialize(
            "topic",
            """
            {
                "FOO": "\u0000\u0000"
            }
            """.trimIndent().toByteArray(),
        )
        result.jsonObject.getValue("FOO").jsonPrimitive.content shouldBe ""
    }
})
