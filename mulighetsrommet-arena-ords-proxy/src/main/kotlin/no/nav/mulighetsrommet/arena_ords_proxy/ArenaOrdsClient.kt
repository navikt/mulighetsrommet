package no.nav.mulighetsrommet.arena_ords_proxy

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class ArenaOrdsClient(private val config: ArenaOrdsConfig) {

    private val logger = LoggerFactory.getLogger(ArenaOrdsClient::class.java)
    private val client: HttpClient
    private val token = "token her"

    init {
        logger.debug("Init ArenaOrdsClient")
        client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
            expectSuccess = true
            defaultRequest {
                // TODO: Sette denne per request når vi får credentials til ords.
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                }
                url.takeFrom(
                    URLBuilder().takeFrom(config.url).apply {
                        encodedPath += url.encodedPath
                    }
                )
            }
        }
    }

    suspend fun getFnrByArenaPersonId(arenaPersonIdList: ArenaPersonIdList): ArenaPersonIdList {
        val response = client.request("/arena/api/v1/person/identListe") {
            method = HttpMethod.Post
            setBody(arenaPersonIdList)
        }
        return response.body()
    }

    suspend fun getArbeidsgiverInfoByArenaArbeidsgiverId(arenaArbeidsgiverId: Int): ArbeidsgiverInfo {
        val response = client.request("/arena/api/v1/arbeidsgiver/ident") {
            headers {
                append("arbgivId", arenaArbeidsgiverId.toString())
            }
            method = HttpMethod.Get
        }
        return response.body()
    }
}
