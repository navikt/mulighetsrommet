package no.nav.mulighetsrommet.api.tilsagn.mapper

import no.nav.mulighetsrommet.altinn.AltinnAttachmentInitRequest
import no.nav.mulighetsrommet.altinn.AltinnCorrespondenceBase
import no.nav.mulighetsrommet.altinn.AltinnCorrespondenceContent
import no.nav.mulighetsrommet.altinn.AltinnCorrespondenceNotification
import no.nav.mulighetsrommet.altinn.AltinnCorrespondenceRequest
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import java.security.MessageDigest
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

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

    fun tilAltinnVedlegg(tilsagn: Tilsagn, personalia: Personalia, pdf: ByteArray): AltinnAttachmentInitRequest {
        val checksum = MessageDigest.getInstance("MD5").digest(pdf).joinToString("") { "%02x".format(it) }
        return AltinnAttachmentInitRequest(
            resourceId = RESOURCE_ID,
            fileName = "Tilsagnsbrev-${tilsagn.tiltakstype.navn}.pdf",
            displayName = createMessageTitle(tilsagn, personalia),
            isEncrypted = false,
            sendersReference = tilsagn.bestilling.bestillingsnummer,
            checksum = checksum,
        )
    }

    fun tilAltinnKorrespondanse(
        tilsagn: Tilsagn,
        personalia: Personalia,
        vedleggId: UUID,
        idempotentKey: UUID,
    ): AltinnCorrespondenceRequest = AltinnCorrespondenceRequest(
        correspondence = createCorrespondenceBase(tilsagn, personalia),
        recipients = listOf(RECIPIENT_PREFIX + tilsagn.arrangor.organisasjonsnummer.value),
        existingAttachments = listOf(vedleggId),
        idempotentKey = idempotentKey,
    )

    private fun createCorrespondenceBase(tilsagn: Tilsagn, personalia: Personalia): AltinnCorrespondenceBase {
        val requestedPublishTime = Instant.now()
        val allowSystemDeleteAfter = OffsetDateTime.ofInstant(requestedPublishTime, ZoneOffset.UTC)
            .plusYears(10)
            .toInstant()

        return AltinnCorrespondenceBase(
            resourceId = RESOURCE_ID,
            sendersReference = tilsagn.bestilling.bestillingsnummer,
            messageSender = MSG_SENDER,
            content = createContent(tilsagn, personalia),
            requestedPublishTime = requestedPublishTime,
            notification = createNotification(tilsagn),
            allowSystemDeleteAfter = allowSystemDeleteAfter,
        )
    }

    private fun createContent(tilsagn: Tilsagn, personalia: Personalia) = AltinnCorrespondenceContent(
        language = LANGUAGE_CODE,
        messageTitle = createMessageTitle(tilsagn, personalia),
        messageBody = "Se vedlagt tilsagnsbrev for ${tilsagn.gjennomforing.navn}.",
    )

    private fun createNotification(tilsagn: Tilsagn): AltinnCorrespondenceNotification {
        val emne = "Tilsagnsbrev for ${tilsagn.arrangor.organisasjonsnummer.value} ${tilsagn.arrangor.navn} er tilgjengelig i Altinn"

        return AltinnCorrespondenceNotification(
            notificationTemplate = NOTIFICATION_TEMPLATE,
            notificationChannel = NOTIFICATION_CHANNEL,
            emailSubject = emne,
            emailBody = createEmailBody(tilsagn),
            sendReminder = false,
        )
    }

    private fun createEmailBody(tilsagn: Tilsagn): String = "Tilsagnsbrev for ${tilsagn.arrangor.organisasjonsnummer.value} ${tilsagn.arrangor.navn} er tilgjengelig. " +
        "Logg inn i Altinn for å se innholdet.\n\nVennlig hilsen Nav"

    private fun createMessageTitle(tilsagn: Tilsagn, personalia: Personalia): String = "Tilsagnsbrev ${tilsagn.tiltakstype.navn} ${personalia.navn()}"
}
