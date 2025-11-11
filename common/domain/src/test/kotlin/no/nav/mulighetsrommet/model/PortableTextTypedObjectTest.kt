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

    test("slate block med tekst til portable text block") {
        val slateBlockJson = """
            {"_type": "block","_key": null,"children": [{"text": "Språknivå A1 og A2 defineres slik av Direktoratet for høyere utdanning og kompetanse (HK-dir):","_type": "span", "marks": ["strong"]}],"markDefs": []}
        """.trimIndent()
        val slateBlocks = Json.decodeFromString<PortableTextTypedObject>(slateBlockJson)
        val portableBlock = slateBlocks.fromSlateFormat()

        assertNotNull<String>(portableBlock._key)
        assertNotNull(portableBlock.additionalProperties["style"])

        val children = portableBlock.additionalProperties["children"]?.let { it as? JsonArray }?.map { it as JsonObject }
        children!!.shouldHaveSize(1)
        val child = children.first()
        child["marks"]!!.jsonArray.first().jsonPrimitive.content.shouldBe("strong")
    }

    test("slate block med lenke til portable text block") {
        val slateBlockJson = """
            {"_key": null,"_type": "block","children":[{"text": "HK-dir sin ", "_type": "span" },{"text": "nettside","_type": "span","marks": ["https://prove.hkdir.no/norskprove-a1-b2"]},{"text": " inneholder nyttig informasjon om språknivå og norskprøver. Her finner du nærmere beskrivelse av alle språknivåene. I tillegg er det øvelsesoppgaver som kan gjøre det lettere for deltaker og Nav-veileder å finne ut hvilket nivå deltaker ligger på.","_type": "span"}],"markDefs": [{"_key": "https://prove.hkdir.no/norskprove-a1-b2","href": "https://prove.hkdir.no/norskprove-a1-b2","_type": "link"}]}
        """.trimIndent()
        val slateBlocks = Json.decodeFromString<PortableTextTypedObject>(slateBlockJson)

        val portableBlock = slateBlocks.fromSlateFormat()

        val markDefKey = portableBlock.additionalProperties["markDefs"]?.let { it as? JsonArray }
            ?.let { it.first() as? JsonObject }?.let { it["_key"] }
        assertNotNull(markDefKey)
        markDefKey!!.jsonPrimitive.content.shouldNotContain("http")

        val children = portableBlock.additionalProperties["children"]?.let { it as? JsonArray }?.map { it as JsonObject }
        val linkMarkedChild = children?.find { it["marks"] != null }
        assertNotNull(linkMarkedChild)
        linkMarkedChild!!["marks"].let { it as JsonArray }.first().shouldBe(markDefKey)
    }

    test("slate block med bullet list til portable text block") {
        val slateBlockJson = """
            {"_type": "block","_key": null,"children": [{"text": "Arbeidssøkerne må ha språknivå A1-A2 ved oppstart til kurs.","_type": "span"}],"listItem": "bullet","markDefs": []}
        """.trimIndent()
        val slateBlocks = Json.decodeFromString<PortableTextTypedObject>(slateBlockJson)

        val portableBlock = slateBlocks.fromSlateFormat()

        assertNotNull(portableBlock.additionalProperties["level"])
    }
})
