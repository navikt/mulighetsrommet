package no.nav.mulighetsrommet.altinn

import com.diffplug.selfie.coroutines.expectSelfie
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.ktor.createMockEngine
import no.nav.mulighetsrommet.ktor.respondJson
import java.time.Instant
import java.util.UUID

class AltinnCorrespondenceClientTest : FunSpec({
    val attachmentId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val correspondenceId = UUID.fromString("22222222-2222-2222-2222-222222222222")
    val idempotentKey = UUID.fromString("33333333-3333-3333-3333-333333333333")

    val initRequest = AltinnAttachmentInitRequest(
        resourceId = "nav_tiltak_tilskuddsbrev",
        fileName = "Bekreftelsesbrev.pdf",
        displayName = "Bekreftelsesbrev",
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
                messageTitle = "Bekreftelsesbrev",
                messageBody = "Se vedlagt bekreftelsesbrev.",
            ),
            requestedPublishTime = Instant.parse("2026-01-01T00:00:00Z"),
            notification = AltinnCorrespondenceNotification(
                notificationTemplate = "CustomMessage",
                notificationChannel = "Email",
                emailSubject = "Bekreftelsesbrev tilgjengelig",
                emailBody = "Se Altinn",
            ),
            allowSystemDeleteAfter = Instant.parse("2036-01-01T00:00:00Z"),
        ),
        recipients = listOf("urn:altinn:organization:identifier-no:999999999"),
        existingAttachments = listOf(attachmentId),
        idempotentKey = idempotentKey,
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

        val error = client.sendVedlegg(initRequest, "pdf".toByteArray()).shouldBeLeft()
        expectSelfie(error.message).toBe("Altinn feilet under virusskanning av vedlegg 11111111-1111-1111-1111-111111111111 (status: Failed)")
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

        val error = client.sendVedlegg(initRequest, "pdf".toByteArray()).shouldBeLeft()
        expectSelfie(error.message).toBe("Feil fra Altinn ved initialisering av vedlegg: 500 Internal Server Error")
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

        val error = client.sendKorrespondanse(korrespondanseRequest).shouldBeLeft()
        expectSelfie(error.message).toBe("Tomt svar fra Altinn korrespondanse-API")
    }

    test("sendKorrespondanse finner eksisterende korrespondanse ved konflikt på idempotensnøkkel") {
        val engine = createMockEngine {
            post("/correspondence/api/v1/correspondence") {
                respondJson("{}", status = HttpStatusCode.Conflict)
            }
            get("/correspondence/api/v1/correspondence?resourceId=nav_tiltak_tilskuddsbrev&role=Sender&idempotentKey=33333333-3333-3333-3333-333333333333") {
                respondJson("""{"ids":["22222222-2222-2222-2222-222222222222"]}""")
            }
        }

        val client = AltinnCorrespondenceClient(
            baseUrl = "https://altinn.no",
            tokenProvider = { "token" },
            clientEngine = engine,
        )

        client.sendKorrespondanse(korrespondanseRequest).shouldBeRight(correspondenceId)
    }

    test("sendKorrespondanse returnerer feil når konflikt ikke finner en eksisterende korrespondanse") {
        val engine = createMockEngine {
            post("/correspondence/api/v1/correspondence") {
                respondJson("{}", status = HttpStatusCode.Conflict)
            }
            get("/correspondence/api/v1/correspondence") {
                respondJson("""{"ids":[]}""")
            }
        }

        val client = AltinnCorrespondenceClient(
            baseUrl = "https://altinn.no",
            tokenProvider = { "token" },
            clientEngine = engine,
        )

        val error = client.sendKorrespondanse(korrespondanseRequest).shouldBeLeft()
        expectSelfie(error.message).toBe("Fant ikke entydig korrespondanse for idempotensnøkkel 33333333-3333-3333-3333-333333333333")
    }

    test("sendKorrespondanse returnerer feil når oppslag etter konflikt feiler") {
        val engine = createMockEngine {
            post("/correspondence/api/v1/correspondence") {
                respondJson("{}", status = HttpStatusCode.Conflict)
            }
            get("/correspondence/api/v1/correspondence") {
                respondJson("{}", status = HttpStatusCode.InternalServerError)
            }
        }

        val client = AltinnCorrespondenceClient(
            baseUrl = "https://altinn.no",
            tokenProvider = { "token" },
            clientEngine = engine,
        )

        val error = client.sendKorrespondanse(korrespondanseRequest).shouldBeLeft()
        expectSelfie(error.message).toBe("Feil fra Altinn ved henting av eksisterende korrespondanse: 500 Internal Server Error")
    }
})
