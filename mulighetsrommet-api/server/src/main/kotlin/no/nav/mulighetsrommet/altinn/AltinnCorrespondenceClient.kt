package no.nav.mulighetsrommet.altinn

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.serializers.OffsetDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.M2MTokenProvider
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/**
 * Klient for Altinn 3 sitt Correspondence-API, som brukes til å sende meldinger (med vedlegg)
 * til virksomheter via Altinn.
 *
 * Se https://docs.altinn.studio/correspondence/
 */
class AltinnCorrespondenceClient(
    private val baseUrl: String,
    private val tokenProvider: M2MTokenProvider,
    clientEngine: HttpClientEngine,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client = httpJsonClient(clientEngine).config {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
    }

    /**
     * Initialiserer, laster opp, og venter til vedlegget er ferdig virussjekket (Published) hos Altinn.
     */
    suspend fun sendVedlegg(
        request: AltinnAttachmentInitRequest,
        pdf: ByteArray,
    ): Either<AltinnCorrespondenceError, UUID> {
        return initierVedlegg(request)
            .flatMap { attachmentId ->
                lastOppVedlegg(attachmentId, pdf).map { attachmentId }
            }
            .flatMap { attachmentId ->
                ventTilVedleggErPublisert(attachmentId).map { attachmentId }
            }
    }

    suspend fun sendKorrespondanse(
        request: AltinnCorrespondenceRequest,
    ): Either<AltinnCorrespondenceError, UUID> {
        val response = client.post("$baseUrl/correspondence/api/v1/correspondence") {
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            log.warn("Feil fra Altinn ved oppretting av korrespondanse: ${response.status} ${response.bodyAsText()}")
            return AltinnCorrespondenceError("Feil fra Altinn ved oppretting av korrespondanse: ${response.status}").left()
        }

        val body = response.body<AltinnCorrespondenceResponse>()
        val correspondenceId = body.correspondences.firstOrNull()?.correspondenceId
            ?: return AltinnCorrespondenceError("Tomt svar fra Altinn korrespondanse-API").left()

        return correspondenceId.right()
    }

    private suspend fun initierVedlegg(request: AltinnAttachmentInitRequest): Either<AltinnCorrespondenceError, UUID> {
        val response = client.post("$baseUrl/correspondence/api/v1/attachment") {
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            log.warn("Feil fra Altinn ved initialisering av vedlegg: ${response.status} ${response.bodyAsText()}")
            return AltinnCorrespondenceError("Feil fra Altinn ved initialisering av vedlegg: ${response.status}").left()
        }

        return UUID.fromString(response.bodyAsText().trim('"')).right()
    }

    private suspend fun lastOppVedlegg(attachmentId: UUID, pdf: ByteArray): Either<AltinnCorrespondenceError, Unit> {
        val response = client.post("$baseUrl/correspondence/api/v1/attachment/$attachmentId/upload") {
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
            contentType(ContentType.Application.OctetStream)
            setBody(pdf)
        }

        if (!response.status.isSuccess()) {
            log.warn("Feil fra Altinn ved opplasting av vedlegg $attachmentId: ${response.status} ${response.bodyAsText()}")
            return AltinnCorrespondenceError("Feil fra Altinn ved opplasting av vedlegg: ${response.status}").left()
        }

        return Unit.right()
    }

    private suspend fun ventTilVedleggErPublisert(attachmentId: UUID): Either<AltinnCorrespondenceError, Unit> {
        var intervalMs = ATTACHMENT_POLL_INITIAL_INTERVAL_MS
        repeat(ATTACHMENT_POLL_MAX_ATTEMPTS) { attempt ->
            val response = client.get("$baseUrl/correspondence/api/v1/attachment/$attachmentId") {
                bearerAuth(tokenProvider.exchange(AccessType.M2M))
            }

            if (!response.status.isSuccess()) {
                log.warn("Feil fra Altinn ved henting av vedlegg-status for $attachmentId: ${response.status} ${response.bodyAsText()}")
                return AltinnCorrespondenceError("Feil fra Altinn ved henting av vedlegg-status: ${response.status}").left()
            }

            val status = response.body<AltinnVedleggStatusResponse>().status
            log.debug("Vedlegg $attachmentId status: $status (forsøk ${attempt + 1}/$ATTACHMENT_POLL_MAX_ATTEMPTS)")

            when (status) {
                ATTACHMENT_STATUS_PUBLISHED -> return Unit.right()

                ATTACHMENT_STATUS_FAILED -> return AltinnCorrespondenceError(
                    "Altinn feilet under virusskanning av vedlegg $attachmentId (status: $status)",
                ).left()
            }

            val jitter = (intervalMs * ATTACHMENT_POLL_JITTER_FACTOR * Random.nextDouble()).toLong()
            delay((intervalMs + jitter).milliseconds)
            intervalMs = min((intervalMs * ATTACHMENT_POLL_BACKOFF_MULTIPLIER).toLong(), ATTACHMENT_POLL_MAX_INTERVAL_MS)
        }

        return AltinnCorrespondenceError(
            "Tidsavbrudd: vedlegg $attachmentId ble ikke publisert etter $ATTACHMENT_POLL_MAX_ATTEMPTS forsøk",
        ).left()
    }

    companion object {
        private const val ATTACHMENT_STATUS_PUBLISHED = "Published"
        private const val ATTACHMENT_STATUS_FAILED = "Failed"
        private const val ATTACHMENT_POLL_INITIAL_INTERVAL_MS = 500L
        private const val ATTACHMENT_POLL_MAX_INTERVAL_MS = 10_000L
        private const val ATTACHMENT_POLL_MAX_ATTEMPTS = 15
        private const val ATTACHMENT_POLL_BACKOFF_MULTIPLIER = 2.0
        private const val ATTACHMENT_POLL_JITTER_FACTOR = 0.5
    }
}

data class AltinnCorrespondenceError(val message: String)

@Serializable
data class AltinnAttachmentInitRequest(
    val resourceId: String,
    val fileName: String,
    val displayName: String,
    val isEncrypted: Boolean = false,
    val sendersReference: String,
    val checksum: String,
)

@Serializable
data class AltinnVedleggStatusResponse(
    val status: String,
)

@Serializable
data class AltinnCorrespondenceRequest(
    val correspondence: AltinnCorrespondenceBase,
    val recipients: List<String>,
    val existingAttachments: List<
        @Serializable(with = UUIDSerializer::class)
        UUID,
        >,
    @Serializable(with = UUIDSerializer::class)
    val idempotentKey: UUID,
)

@Serializable
data class AltinnCorrespondenceBase(
    val resourceId: String,
    val sendersReference: String,
    val messageSender: String,
    val content: AltinnCorrespondenceContent,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val requestedPublishTime: OffsetDateTime,
    val notification: AltinnCorrespondenceNotification,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val allowSystemDeleteAfter: OffsetDateTime,
)

@Serializable
data class AltinnCorrespondenceContent(
    val language: String,
    val messageTitle: String,
    val messageBody: String,
)

@Serializable
data class AltinnCorrespondenceNotification(
    val notificationTemplate: String,
    val notificationChannel: String,
    val emailSubject: String,
    val emailBody: String,
    val sendReminder: Boolean = false,
)

@Serializable
data class AltinnCorrespondenceResponse(
    val correspondences: List<Correspondence>,
) {
    @Serializable
    data class Correspondence(
        @Serializable(with = UUIDSerializer::class)
        val correspondenceId: UUID,
    )
}
