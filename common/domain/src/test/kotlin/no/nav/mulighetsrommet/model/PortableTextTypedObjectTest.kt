package no.nav.mulighetsrommet.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.intellij.lang.annotations.Language

class PortableTextTypedObjectTest : FunSpec({

    @Language("JSON")
    val jsonBlockContent = """
      {
        "_type": "block",
        "_key": "abc123",
        "style": "normal",
        "children": [
          {
            "text": "Dette er tekst i en block.",
            "_type": "span"
          }
        ]
      }
    """.trimIndent()

    val blockContent = PortableTextTypedObject(
        _type = "block",
        _key = "abc123",
        additionalProperties = mapOf(
            "style" to JsonPrimitive("normal"),
            "children" to JsonArray(
                listOf(
                    JsonObject(
                        mapOf(
                            "text" to JsonPrimitive("Dette er tekst i en block."),
                            "_type" to JsonPrimitive("span"),
                        ),
                    ),
                ),
            ),
        ),
    )

    @OptIn(ExperimentalSerializationApi::class)
    val jsonPrettyPrint = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    test("Deserialisering av enkel PortableTextTypedObject") {
        val decoded = Json.decodeFromString<PortableTextTypedObject>(jsonBlockContent)
        decoded shouldBe blockContent
    }

    test("Serialisering av enkel PortableTextTypedObject") {
        val encoded = jsonPrettyPrint.encodeToString(PortableTextTypedObject.serializer(), blockContent)
        encoded shouldBe jsonBlockContent
    }

    test("Deserialisering av PortableTextTypedObject med null _key") {
        val jsonWithNullKey = """
            {
              "_type": "block",
              "_key": null,
              "style": "normal"
            }
        """.trimIndent()

        val expectedObj = PortableTextTypedObject(
            _type = "block",
            _key = null,
            additionalProperties = mapOf(
                "style" to JsonPrimitive("normal"),
            ),
        )

        val decoded = Json.decodeFromString<PortableTextTypedObject>(jsonWithNullKey)
        decoded shouldBe expectedObj
    }
})
