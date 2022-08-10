package no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter.consumers.helpers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.arena.adapter.consumers.helpers.ArenaEventHelpers
import org.intellij.lang.annotations.Language

class ArenaEventHelpersTest : FunSpec({

    @Serializable
    data class Foo(val name: String)

    context("decodeAfter") {
        test("should decode 'after' block to specified type") {
            @Language("JSON")
            val data = """
            {
                "after": {
                    "name": "Bar"
                }
            }
            """.trimIndent()

            val decoded = ArenaEventHelpers.decodeAfter<Foo>(Json.parseToJsonElement(data))

            decoded shouldBe Foo(name = "Bar")
        }
    }
})
