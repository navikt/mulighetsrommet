package no.nav.mulighetsrommet.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldHave
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import no.nav.common.utils.AssertUtils.assertNotNull
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
