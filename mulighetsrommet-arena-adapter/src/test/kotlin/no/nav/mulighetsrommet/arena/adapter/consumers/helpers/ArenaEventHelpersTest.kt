package no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter.consumers.helpers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.arena.adapter.consumers.helpers.ArenaEventHelpers
import no.nav.mulighetsrommet.arena.adapter.consumers.helpers.ArenaOperation
import org.intellij.lang.annotations.Language

class ArenaEventHelpersTest : FunSpec({

    @Serializable
    data class Foo(val name: String)

    context("decodeEvent") {
        test("should decode arena operation") {
            @Language("JSON")
            val data = """
            {
                "op_type": "I",
                "after": {
                    "name": "Bar"
                }
            }
            """.trimIndent()

            val decoded = ArenaEventHelpers.decodeEvent<Foo>(Json.parseToJsonElement(data))

            decoded.operation shouldBe ArenaOperation.Insert
        }

        test("should decode 'after' block to specified type") {
            @Language("JSON")
            val data = """
            {
                "op_type": "I",
                "after": {
                    "name": "Bar"
                }
            }
            """.trimIndent()

            val decoded = ArenaEventHelpers.decodeEvent<Foo>(Json.parseToJsonElement(data))

            decoded.data shouldBe Foo(name = "Bar")
        }
    }
})
