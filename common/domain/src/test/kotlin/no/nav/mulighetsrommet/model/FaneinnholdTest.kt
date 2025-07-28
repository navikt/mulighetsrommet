package no.nav.mulighetsrommet.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.intellij.lang.annotations.Language

class FaneinnholdTest : FunSpec({

    @Language("JSON")
    val faneinnholdJsonContent = """
      {
        "kontaktinfo": [
          {
            "_type": "block",
            "_key": null,
            "style": "normal",
            "children": [
              {
                "text": "Oppmøtested er Andeby 1, 0669 Donald-Pocketby",
                "_type": "span"
              }
            ]
          }
        ],
        "lenker": [
          {
            "lenkenavn": "Søk via Google",
            "lenke": "https://www.google.com",
            "apneINyFane": true,
            "visKunForVeileder": false
          }
        ]
      }
    """.trimIndent()

    val faneinnholdDataClass = Faneinnhold(

        kontaktinfo = listOf(
            PortableTextTypedObject(
                _type = "block",
                _key = null,
                additionalProperties = mapOf(
                    "style" to JsonPrimitive("normal"),
                    "children" to JsonArray(
                        listOf(
                            JsonObject(
                                mapOf(
                                    "text" to JsonPrimitive("Oppmøtested er Andeby 1, 0669 Donald-Pocketby"),
                                    "_type" to JsonPrimitive("span"),
                                ),
                            ),
                        ),
                    ),
                ),
            ),

        ),
        lenker = listOf(
            FaneinnholdLenke(
                lenke = "https://www.google.com",
                lenkenavn = "Søk via Google",
                apneINyFane = true,
                visKunForVeileder = false,
            ),
        ),
    )

    @OptIn(ExperimentalSerializationApi::class)
    val jsonPrettyPrint = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    test("Deserializing Faneinnhold from JSON") {
        val decodeFromString = Json.decodeFromString<Faneinnhold>(faneinnholdJsonContent)
        decodeFromString shouldBe faneinnholdDataClass
    }

    test("Serializing Faneinnhold to JSON") {
        val encodeToString = jsonPrettyPrint.encodeToString(faneinnholdDataClass)
        encodeToString shouldBe faneinnholdJsonContent
    }
})
