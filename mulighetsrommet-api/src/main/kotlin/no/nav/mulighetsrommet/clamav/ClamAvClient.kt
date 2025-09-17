package no.nav.mulighetsrommet.clamav

import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.securelog.SecureLog

class ClamAvClient(
    private val baseUrl: String,
    clientEngine: HttpClientEngine,
) {
    private val log = SecureLog.logger
    private val client = httpJsonClient(clientEngine).config {
        install(HttpCache)
    }

    suspend fun virusScanVedlegg(vedleggList: List<Vedlegg>): List<ScanResult> {
        log.info("Scanner ${vedleggList.size} vedlegg for virus")
        val httpResponse =
            client.submitFormWithBinaryData(
                url = "$baseUrl/scan",
                formData =
                formData {
                    vedleggList.forEachIndexed { index, vedlegg ->
                        append(
                            "file$index",
                            vedlegg.content.content,
                            Headers.build {
                                append(HttpHeaders.ContentType, vedlegg.content.contentType)
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "filename=${vedlegg.filename.replace("\n", "")}",
                                )
                            },
                        )
                    }
                },
            )
        return httpResponse.body<List<ScanResult>>()
    }
}

@Serializable
data class ScanResult(
    val Filename: String,
    val Result: Status,
)

@Serializable
enum class Status {
    FOUND,
    OK,
    ERROR,
}

@Serializable
data class Vedlegg(
    val content: Content,
    val filename: String,
)

@Serializable
data class Content(val contentType: String, val content: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Content

        if (contentType != other.contentType) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contentType.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }
}
