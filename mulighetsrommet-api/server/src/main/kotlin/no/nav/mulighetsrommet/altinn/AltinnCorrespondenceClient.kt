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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.ktor.clients.httpJsonClient
import no.nav.mulighetsrommet.serializers.InstantSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.tokenprovider.M2MTokenProvider
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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

    /**
     * Poller vedlegg-status med et fast intervall inntil vedlegget er ferdig virussjekket
     * (Published/Failed), eller til [ATTACHMENT_POLL_TIMEOUT] er nådd. Selve tasken som bruker
     * klienten har sin egen retry med backoff (se SendTilsagnsbrevTilAltinn), så det er ikke
     * behov for noen mer avansert backoff-strategi her.
     */
    private suspend fun ventTilVedleggErPublisert(attachmentId: UUID): Either<AltinnCorrespondenceError, Unit> {
        val sluttstatus = withTimeoutOrNull(ATTACHMENT_POLL_TIMEOUT) {
            var status = hentVedleggStatus(attachmentId)
            while (status is AttachmentStatus.Processing) {
                delay(ATTACHMENT_POLL_INTERVAL)
                status = hentVedleggStatus(attachmentId)
            }
            status
        } ?: AttachmentStatus.Processing

        return when (sluttstatus) {
            is AttachmentStatus.Published -> Unit.right()

            is AttachmentStatus.Failed -> sluttstatus.error.left()

            is AttachmentStatus.Processing -> AltinnCorrespondenceError(
                "Tidsavbrudd: vedlegg $attachmentId ble ikke publisert innen $ATTACHMENT_POLL_TIMEOUT",
            ).left()
        }
    }

    private suspend fun hentVedleggStatus(attachmentId: UUID): AttachmentStatus {
        val response = client.get("$baseUrl/correspondence/api/v1/attachment/$attachmentId") {
            bearerAuth(tokenProvider.exchange(AccessType.M2M))
        }

        if (!response.status.isSuccess()) {
            log.warn("Feil fra Altinn ved henting av vedlegg-status for $attachmentId: ${response.status} ${response.bodyAsText()}")
            return AttachmentStatus.Failed(
                AltinnCorrespondenceError("Feil fra Altinn ved henting av vedlegg-status: ${response.status}"),
            )
        }

        val status = response.body<AltinnVedleggStatusResponse>().status
        log.debug("Vedlegg $attachmentId status: $status")

        return when (status) {
            ATTACHMENT_STATUS_PUBLISHED -> AttachmentStatus.Published

            ATTACHMENT_STATUS_FAILED -> AttachmentStatus.Failed(
                AltinnCorrespondenceError("Altinn feilet under virusskanning av vedlegg $attachmentId (status: $status)"),
            )

            else -> AttachmentStatus.Processing
        }
    }

    /**
     * Resultatet av å spørre Altinn om status for et vedlegg som er lastet opp.
     */
    private sealed interface AttachmentStatus {
        data object Published : AttachmentStatus
        data object Processing : AttachmentStatus
        data class Failed(val error: AltinnCorrespondenceError) : AttachmentStatus
    }

    companion object {
        private const val ATTACHMENT_STATUS_PUBLISHED = "Published"
        private const val ATTACHMENT_STATUS_FAILED = "Failed"
        private val ATTACHMENT_POLL_INTERVAL = 1.seconds
        private val ATTACHMENT_POLL_TIMEOUT = 2.minutes
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
    @Serializable(with = InstantSerializer::class)
    val requestedPublishTime: Instant,
    val notification: AltinnCorrespondenceNotification,
    @Serializable(with = InstantSerializer::class)
    val allowSystemDeleteAfter: Instant,
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
