package no.nav.mulighetsrommet.arena.adapter.no.nav.mulighetsrommet.arena.adapter.consumers.helpers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.arena.adapter.consumers.helpers.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.consumers.helpers.ArenaEventHelpers
import no.nav.mulighetsrommet.arena.adapter.consumers.helpers.ArenaOperation

class ArenaEventHelpersTest : FunSpec({

    @Serializable
    data class Foo(val name: String)

    context("decodeEvent") {

        fun createData(operation: String): String {
            return """
                {
                    "op_type": "$operation",
                    "before": {
                        "name": "Foo"
                    },
                    "after": {
                        "name": "Bar"
                    }
                }
            """.trimIndent()
        }

        test("should decode arena operation") {
            val data = createData("I")

            val decoded = ArenaEventHelpers.decodeEvent<Foo>(Json.parseToJsonElement(data))

            decoded.operation shouldBe ArenaOperation.Insert
        }

        context("when operation is Insert") {
            test("should decode 'after' block to specified type") {
                val data = createData("I")

                val decoded = ArenaEventHelpers.decodeEvent<Foo>(Json.parseToJsonElement(data))

                decoded shouldBe ArenaEvent(ArenaOperation.Insert, Foo(name = "Bar"))
            }
        }

        context("when operation is Update") {
            test("should decode 'after' block to specified type") {
                val data = createData("U")

                val decoded = ArenaEventHelpers.decodeEvent<Foo>(Json.parseToJsonElement(data))

                decoded shouldBe ArenaEvent(ArenaOperation.Update, Foo(name = "Bar"))
            }
        }

        context("when operation is Delete") {
            test("should decode 'before' block to specified type") {
                val data = createData("D")

                val decoded = ArenaEventHelpers.decodeEvent<Foo>(Json.parseToJsonElement(data))

                decoded shouldBe ArenaEvent(ArenaOperation.Delete, Foo(name = "Foo"))
            }
        }
    }
})
