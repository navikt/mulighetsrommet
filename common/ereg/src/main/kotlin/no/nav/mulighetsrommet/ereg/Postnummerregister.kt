package no.nav.mulighetsrommet.ereg

import kotlinx.serialization.json.Json

/**
 * Slår opp poststed for norske postnumre, basert på Posten/Brings postnummerregister [0].
 *
 * Ereg oppgir kun poststed for utenlandske adresser (i hvert fall foreløpig), så for
 * norske adresser må poststed utledes på egenhånd.
 *
 * [0]: https://www.bring.no/tjenester/adressetjenester/postnummer/postnummertabeller-veiledning
 */
object Postnummerregister {
    private val poststedByPostnummer: Map<String, String> by lazy { load() }

    fun poststed(postnummer: String): String? = poststedByPostnummer[postnummer]

    private fun load(): Map<String, String> {
        val resource = requireNotNull(javaClass.getResourceAsStream("/postnummerregister.json")) {
            "Fant ikke postnummerregister.json på classpath"
        }
        val json = resource.bufferedReader().use { it.readText() }
        return Json.decodeFromString(json)
    }
}
