package no.nav.mulighetsrommet.altinn

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import java.time.OffsetDateTime
import java.util.UUID

class AltinnCorrespondenceClientTest : FunSpec({
    val attachmentId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val correspondenceId = UUID.fromString("22222222-2222-2222-2222-222222222222")

    val initRequest = AltinnAttachmentInitRequest(
        resourceId = "nav_tiltak_tilskuddsbrev",
        fileName = "Tilsagnsbrev.pdf",
        displayName = "Tilsagnsbrev",
        sendersReference = "ref-1",
        checksum = "checksum",
    )

    val korrespondanseRequest = AltinnCorrespondenceRequest(
        correspondence = AltinnCorrespondenceBase(
            resourceId = "nav_tiltak_tilskuddsbrev",
            sendersReference = "ref-1",
            messageSender = "NAV",
            content = AltinnCorrespondenceContent(
                language = "nb",
                messageTitle = "Tilsagnsbrev",
                messageBody = "Se vedlagt tilsagnsbrev.",
            ),
            requestedPublishTime = OffsetDateTime.now(),
            notification = AltinnCorrespondenceNotification(
                notificationTemplate = "CustomMessage",
                notificationChannel = "Email",
                emailSubject = "Tilsagnsbrev tilgjengelig",
                emailBody = "Se Altinn",
            ),
            allowSystemDeleteAfter = OffsetDateTime.now().plusYears(10),
        ),
        recipients = listOf("urn:altinn:organization:identifier-no:999999999"),
        existingAttachments = listOf(attachmentId),
        idempotentKey = UUID.randomUUID(),
    )

    test("sendVedlegg initierer, laster opp og venter til vedlegg er publisert") {
        val engine = createMockEngine {
            post("/correspondence/api/v1/attachment") {
                respondJson("\"$attachmentId\"")
            }
            post("/correspondence/api/v1/attachment/$attachmentId/upload") {
                respondJson(Unit)
            }
            get("/correspondence/api/v1/attachment/$attachmentId") {
                respondJson(AltinnVedleggStatusResponse(status = "Published"))
            }
        }

        val client = AltinnCorrespondenceClient(
            baseUrl = "https://altinn.no",
            tokenProvider = { "token" },
            clientEngine = engine,
        )

        client.sendVedlegg(initRequest, "pdf".toByteArray()).shouldBeRight(attachmentId)
    }

    test("sendVedlegg returnerer feil hvis virusskanning feiler") {
        val engine = createMockEngine {
            post("/correspondence/api/v1/attachment") {
                respondJson("\"$attachmentId\"")
            }
            post("/correspondence/api/v1/attachment/$attachmentId/upload") {
                respondJson(Unit)
            }
            get("/correspondence/api/v1/attachment/$attachmentId") {
                respondJson(AltinnVedleggStatusResponse(status = "Failed"))
            }
        }

        val client = AltinnCorrespondenceClient(
            baseUrl = "https://altinn.no",
            tokenProvider = { "token" },
            clientEngine = engine,
        )

        client.sendVedlegg(initRequest, "pdf".toByteArray()).shouldBeLeft()
    }

    test("sendVedlegg returnerer feil hvis initialisering feiler") {
        val engine = createMockEngine {
            post("/correspondence/api/v1/attachment") {
                respondJson("feil", status = HttpStatusCode.InternalServerError)
            }
        }

        val client = AltinnCorrespondenceClient(
            baseUrl = "https://altinn.no",
            tokenProvider = { "token" },
            clientEngine = engine,
        )

        client.sendVedlegg(initRequest, "pdf".toByteArray()).shouldBeLeft()
    }

    test("sendKorrespondanse returnerer correspondenceId ved suksess") {
        val engine = createMockEngine {
            post("/correspondence/api/v1/correspondence") {
                respondJson(
                    AltinnCorrespondenceResponse(
                        correspondences = listOf(AltinnCorrespondenceResponse.Correspondence(correspondenceId)),
                    ),
                )
            }
        }

        val client = AltinnCorrespondenceClient(
            baseUrl = "https://altinn.no",
            tokenProvider = { "token" },
            clientEngine = engine,
        )

        client.sendKorrespondanse(korrespondanseRequest).shouldBeRight(correspondenceId)
    }

    test("sendKorrespondanse returnerer feil hvis Altinn svarer med tomt resultat") {
        val engine = createMockEngine {
            post("/correspondence/api/v1/correspondence") {
                respondJson(AltinnCorrespondenceResponse(correspondences = emptyList()))
            }
        }

        val client = AltinnCorrespondenceClient(
            baseUrl = "https://altinn.no",
            tokenProvider = { "token" },
            clientEngine = engine,
        )

        client.sendKorrespondanse(korrespondanseRequest).shouldBeLeft()
    }
})
