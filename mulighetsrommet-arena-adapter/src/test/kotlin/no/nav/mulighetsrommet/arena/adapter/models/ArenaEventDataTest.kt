package no.nav.mulighetsrommet.arena.adapter.models

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ArenaEventDataTest : FunSpec({

    @Serializable
    data class Foo(val name: String)

    context("decodeEvent") {

        val table = "foo_table"

        fun createData(operation: String): String {
            return """
                {
                    "table": "$table",
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

            val decoded = ArenaEventData.decode<Foo>(Json.parseToJsonElement(data))

            decoded.operation shouldBe ArenaEventData.Operation.Insert
        }

        context("when operation is Insert") {
            test("should decode 'after' block to specified type") {
                val data = createData("I")

                val decoded = ArenaEventData.decode<Foo>(Json.parseToJsonElement(data))

                decoded shouldBe ArenaEventData(table, ArenaEventData.Operation.Insert, Foo(name = "Bar"))
            }
        }

        context("when operation is Update") {
            test("should decode 'after' block to specified type") {
                val data = createData("U")

                val decoded = ArenaEventData.decode<Foo>(Json.parseToJsonElement(data))

                decoded shouldBe ArenaEventData(table, ArenaEventData.Operation.Update, Foo(name = "Bar"))
            }
        }

        context("when operation is Delete") {
            test("should decode 'before' block to specified type") {
                val data = createData("D")

                val decoded = ArenaEventData.decode<Foo>(Json.parseToJsonElement(data))

                decoded shouldBe ArenaEventData(table, ArenaEventData.Operation.Delete, Foo(name = "Foo"))
            }
        }
    }
})
