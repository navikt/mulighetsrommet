package no.nav.mulighetsrommet.arena_ords_proxy

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class ArenaOrdsClient(private val config: ArenaOrdsConfig) {

    private val logger = LoggerFactory.getLogger(ArenaOrdsClient::class.java)
    private val client: HttpClient
    private val token = "token her"

    init {
        logger.debug("Init ArenaOrdsClient")
        client = HttpClient(CIO) {
            expectSuccess = true
            defaultRequest {
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
            contentType(ContentType.Application.Json)
            method = HttpMethod.Post
            setBody(Json.encodeToString(arenaPersonIdList))
        }
        return response.body()
    }

    suspend fun getArbeidsgiverInfoByArenaArbeidsgiverId(arenaArbeidsgiverId: Int) =
        client.request("/arena/api/v1/arbeidsgiver/ident") {
            contentType(ContentType.Application.Json)
            headers {
                append("arbgivId", arenaArbeidsgiverId.toString())
            }
            method = HttpMethod.Get
        }

}
