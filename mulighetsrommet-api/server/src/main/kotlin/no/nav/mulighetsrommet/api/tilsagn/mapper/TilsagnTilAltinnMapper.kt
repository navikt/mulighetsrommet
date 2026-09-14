package no.nav.mulighetsrommet.api.tilsagn.mapper

import no.nav.mulighetsrommet.altinn.AltinnAttachmentInitRequest
import no.nav.mulighetsrommet.altinn.AltinnCorrespondenceBase
import no.nav.mulighetsrommet.altinn.AltinnCorrespondenceContent
import no.nav.mulighetsrommet.altinn.AltinnCorrespondenceNotification
import no.nav.mulighetsrommet.altinn.AltinnCorrespondenceRequest
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.security.MessageDigest
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

data class TilsagnAltinnSnapshot(
    val tiltakstypeNavn: String,
    val bestillingsnummer: String,
    val arrangorOrganisasjonsnummer: Organisasjonsnummer,
    val arrangorNavn: String,
)

object TilsagnTilAltinnMapper {
    /**
     * ResourceId registrert i Altinns ressursregister for utsending av tilsagnsbrev.
     * Delt med tiltak-tilsagnsbrev ettersom begge sender til samme type mottakere (arrangører)
     * på vegne av Nav.
     */
    const val RESOURCE_ID = "nav_tiltak_tilskuddsbrev"

    private const val LANGUAGE_CODE = "nb"
    private const val MSG_SENDER = "Nav"
    private const val RECIPIENT_PREFIX = "urn:altinn:organization:identifier-no:"
    private const val NOTIFICATION_TEMPLATE = "CustomMessage"
    private const val NOTIFICATION_CHANNEL = "Email"

    fun tilAltinnVedlegg(tilsagn: TilsagnAltinnSnapshot, pdf: ByteArray): AltinnAttachmentInitRequest {
        val checksum = MessageDigest.getInstance("MD5").digest(pdf).joinToString("") { "%02x".format(it) }
        return AltinnAttachmentInitRequest(
            resourceId = RESOURCE_ID,
            fileName = "Tilsagnsbrev-${tilsagn.tiltakstypeNavn}.pdf",
            displayName = createMessageTitle(tilsagn),
            isEncrypted = false,
            sendersReference = tilsagn.bestillingsnummer,
            checksum = checksum,
        )
    }

    fun tilAltinnKorrespondanse(
        tilsagn: TilsagnAltinnSnapshot,
        vedleggId: UUID,
        idempotentKey: UUID,
        now: Instant = Instant.now(),
    ): AltinnCorrespondenceRequest = AltinnCorrespondenceRequest(
        correspondence = createCorrespondenceBase(tilsagn, now),
        recipients = listOf(RECIPIENT_PREFIX + tilsagn.arrangorOrganisasjonsnummer.value),
        existingAttachments = listOf(vedleggId),
        idempotentKey = idempotentKey,
    )

    private fun createCorrespondenceBase(
        tilsagn: TilsagnAltinnSnapshot,
        requestedPublishTime: Instant,
    ): AltinnCorrespondenceBase {
        val allowSystemDeleteAfter = OffsetDateTime.ofInstant(requestedPublishTime, ZoneOffset.UTC)
            .plusYears(10)
            .toInstant()

        return AltinnCorrespondenceBase(
            resourceId = RESOURCE_ID,
            sendersReference = tilsagn.bestillingsnummer,
            messageSender = MSG_SENDER,
            content = createContent(tilsagn),
            requestedPublishTime = requestedPublishTime,
            notification = createNotification(tilsagn),
            allowSystemDeleteAfter = allowSystemDeleteAfter,
        )
    }

    private fun createContent(tilsagn: TilsagnAltinnSnapshot) = AltinnCorrespondenceContent(
        language = LANGUAGE_CODE,
        messageTitle = createMessageTitle(tilsagn),
        messageBody = "Se vedlagt tilsagnsbrev for ${tilsagn.tiltakstypeNavn}.",
    )

    private fun createNotification(tilsagn: TilsagnAltinnSnapshot): AltinnCorrespondenceNotification {
        val emne = """
            Tilsagnsbrev for ${tilsagn.arrangorOrganisasjonsnummer.value} ${tilsagn.arrangorNavn} er tilgjengelig i Altinn
        """.trimIndent()

        return AltinnCorrespondenceNotification(
            notificationTemplate = NOTIFICATION_TEMPLATE,
            notificationChannel = NOTIFICATION_CHANNEL,
            emailSubject = emne,
            emailBody = createEmailBody(tilsagn),
            sendReminder = false,
        )
    }

    private fun createEmailBody(tilsagn: TilsagnAltinnSnapshot): String = """
        Tilsagnsbrev for ${tilsagn.arrangorOrganisasjonsnummer.value} ${tilsagn.arrangorNavn} er tilgjengelig. Logg inn i Altinn for å se innholdet.

        Vennlig hilsen Nav
    """.trimIndent()

    private fun createMessageTitle(tilsagn: TilsagnAltinnSnapshot): String = """
        Tilsagnsbrev ${tilsagn.tiltakstypeNavn} ${tilsagn.bestillingsnummer}
    """.trimIndent()
}
