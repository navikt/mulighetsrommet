package no.nav.mulighetsrommet.api.clients.utdanning

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient

class UtdanningClient(engine: HttpClientEngine = CIO.create(), val config: Config) {
    data class Config(
        val baseurl: String,
    )

    private val client: HttpClient = httpJsonClient(engine)

    suspend fun getUtdanninger(): Flow<Utdanning> = flow {
        val response = client.get("${config.baseurl}/api/v1/data_norge--utdanningsbeskrivelse")
        val utdanninger = response.body<List<String>>()
        utdanninger.forEach {
            val utdanning = getUtdanning(it)
            emit(utdanning)
        }
    }

    private suspend fun getUtdanning(url: String): Utdanning {
        val utdanning = client.get(url).body<Utdanning>()
        return utdanning
    }
}

@Serializable
data class Utdanning(
    val title: String,
    val body: Body,
    val sammenligning_id: String,
    val utdtype: List<Utdanningstype>,
    val nus: List<Nuskodeverk>,
    val interesse: List<Interesse>,
    val sokeord: List<Sokeord>,
) {
    @Serializable
    data class Body(
        val summary: String,
    )

    @Serializable
    data class Utdanningstype(
        val title: String,
        val utdt_kode: String,
    )

    @Serializable
    data class Nuskodeverk(
        val title: String,
        val nus_kode: String,
    )

    @Serializable
    data class Interesse(
        val title: String,
    )

    @Serializable
    data class Sokeord(
        val title: String,
    )
}
