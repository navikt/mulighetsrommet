package no.nav.mulighetsrommet.api.clients.utdanning

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.ktor.clients.ClientResponseMetricPlugin
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory

class UtdanningClient(engine: HttpClientEngine = CIO.create(), private val database: Database, val config: Config) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val baseurl: String,
    )

    private val client: HttpClient = HttpClient(engine) {
        expectSuccess = false

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }

        install(ClientResponseMetricPlugin)

        install(HttpRequestRetry) {
            retryOnException(maxRetries = 3, retryOnTimeout = true)
            exponentialDelay()
            modifyRequest {
                response?.let {
                    logger.warn("Request failed with response status=${it.status}")
                }
                logger.info("Retrying request method=${request.method.value}, url=${request.url.buildString()}")
            }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30000
        }

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                },
            )
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun getUtdanninger(): List<Utdanning> {
        val response = client.get("${config.baseurl}/api/v1/data_norge--utdanningsbeskrivelse")
        val utdanninger = response.body<List<String>>()
        return utdanninger.map { saveUtdanning(it) }
    }

    private suspend fun saveUtdanning(url: String): Utdanning {
        val utdanning = client.get(url).body<Utdanning>()

        val id = utdanning.sammenligning_id.substringAfter("u_")
        val interesser = utdanning.interesse.map { it.title }
        val sokeord = utdanning.sokeord.map { it.title }

        @Language("PostgreSQL")
        val query = """
            insert into utdanning (id, utdanning_no_sammenligning_id, title, description, aktiv, utdanningstype, sokeord, interesser)
            values (:id, :utdanning_no_sammenligning_id, :title, :description, true, :utdanningstype::utdanningstype[], :sokeord, :interesser)
            on conflict (id) do update set
                utdanning_no_sammenligning_id = excluded.utdanning_no_sammenligning_id,
                title = excluded.title,
                description = excluded.description,
                studieretning = excluded.studieretning,
                utdanningstype = excluded.utdanningstype::utdanningstype[],
                sokeord = excluded.sokeord,
                interesser = excluded.interesser
        """.trimIndent()

        @Language("PostgreSQL")
        val nuskodeInnholdInsertQuery = """
            insert into utdanning_nus_kode_innhold(title, nus_kode, aktiv)
            values(:title, :nus_kode, true)
            on conflict (nus_kode) do update set
                title = excluded.title
        """.trimIndent()

        @Language("PostgreSQL")
        val nusKodeKoblingforUtdanningQuery = """
            insert into utdanning_nus_kode(utdanning_id, nus_kode_id)
            values (:utdanning_id, :nus_kode_id)
        """.trimIndent()

        database.transaction { tx ->
            queryOf(
                query,
                mapOf(
                    "id" to id,
                    "utdanning_no_sammenligning_id" to utdanning.sammenligning_id,
                    "title" to utdanning.title,
                    "description" to utdanning.body.summary,
                    "utdanningstype" to database.createArrayOf(
                        "utdanningstype",
                        utdanning.utdtype.map { getUtdanningstype(it.utdt_kode) },
                    ),
                    "sokeord" to database.createTextArray(sokeord),
                    "interesser" to database.createTextArray(interesser),
                ),
            ).asExecute.let { tx.run(it) }

            utdanning.nus.forEach { nus ->
                queryOf(
                    nuskodeInnholdInsertQuery,
                    mapOf("title" to nus.title, "nus_kode" to nus.nus_kode),
                ).asExecute.runWithSession(tx)

                queryOf(
                    nusKodeKoblingforUtdanningQuery,
                    mapOf("utdanning_id" to id, "nus_kode_id" to nus.nus_kode),
                ).asExecute.let { tx.run(it) }
            }
        }
        return utdanning
    }

    private fun getUtdanningstype(utdtKode: String): Utdanningstype {
        return when (utdtKode) {
            "VS" -> Utdanningstype.VIDEREGAENDE
            "UH" -> Utdanningstype.UNIVERSITET_OG_HOGSKOLE
            "TO" -> Utdanningstype.TILSKUDDSORDNING
            "FAG" -> Utdanningstype.FAGSKOLE
            else -> throw IllegalArgumentException("Ukjent utdanningstype")
        }
    }
}

enum class Utdanningstype {
    FAGSKOLE,
    TILSKUDDSORDNING,
    VIDEREGAENDE,
    UNIVERSITET_OG_HOGSKOLE,
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
