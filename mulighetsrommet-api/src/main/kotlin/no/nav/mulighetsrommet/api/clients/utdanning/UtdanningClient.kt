package no.nav.mulighetsrommet.api.clients.utdanning

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient

class UtdanningClient(engine: HttpClientEngine = CIO.create(), val config: Config) {
    data class Config(
        val baseurl: String,
    )

    private val client: HttpClient = httpJsonClient(engine)

    suspend fun getUtdanninger(): List<Utdanning> {
        val response = client.get("${config.baseurl}/nav_export/programomraader")
        return response.body<List<Utdanning>>()
    }
}

@Serializable
data class Utdanning(
    @SerialName("programomradekode10")
    val programomradekode: String,
    @SerialName("utdanningsbeskrivelse_uno_id")
    val utdanningId: String? = null, // null for utdanningsprogram
    @SerialName("programomrade_tittel")
    val navn: String,
    @SerialName("utdanningsprogram_type")
    val utdanningsprogram: Utdanningsprogram? = null,
    val sluttkompetanse: Sluttkompetanse? = null,
    val aktiv: Boolean,
    @SerialName("calculated_status")
    val utdanningstatus: Utdanningstatus,
    @SerialName("canonical_path")
    val utdanningslop: List<String>,
    @SerialName("nus")
    val nusKodeverk: List<Nuskodeverk>,
) {

    @Serializable
    data class Nuskodeverk(
        @SerialName("nus_navn_nb")
        val navn: String,
        @SerialName("nus_kode")
        val kode: String,
    )

    @Serializable
    enum class Utdanningsprogram() {
        @SerialName("Yrkesfaglig utdanningsprogram")
        YRKESFAGLIG,

        @SerialName("Studieforberedende utdanningsprogram")
        STUDIEFORBEREDENDE,
    }

    @Serializable
    enum class Sluttkompetanse() {
        Fagbrev,
        Svennebrev,
        Studiekompetanse,
        Yrkeskompetanse,
    }

    @Serializable
    enum class Utdanningstatus() {
        GYLDIG,
        KOMMENDE,
        UTGAAENDE,
        UTGAATT,
    }
}
