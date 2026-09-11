package no.nav.mulighetsrommet.api.tilsagn.mapper

import no.nav.mulighetsrommet.altinn.AltinnAttachmentInitRequest
import no.nav.mulighetsrommet.altinn.AltinnCorrespondenceBase
import no.nav.mulighetsrommet.altinn.AltinnCorrespondenceContent
import no.nav.mulighetsrommet.altinn.AltinnCorrespondenceNotification
import no.nav.mulighetsrommet.altinn.AltinnCorrespondenceRequest
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

object TilsagnTilAltinnMapper {
    /**
     * ResourceId registrert i Altinns ressursregister for utsending av tilsagnsbrev.
     * Delt med tiltak-tilsagnsbrev ettersom begge sender til samme type mottakere (arrangører)
     * på vegne av Nav.
     */
    const val RESOURCE_ID = "nav_tiltak_tilskuddsbrev"

    private const val LANGUAGE_CODE = "nb"
    private const val MSG_SENDER = "NAV"
    private const val RECIPIENT_PREFIX = "urn:altinn:organization:identifier-no:"
    private const val NOTIFICATION_TEMPLATE = "CustomMessage"
    private const val NOTIFICATION_CHANNEL = "Email"

    fun tilAltinnVedlegg(tilsagn: Tilsagn, personalia: Personalia, pdf: ByteArray): AltinnAttachmentInitRequest {
        val checksum = MessageDigest.getInstance("MD5").digest(pdf).joinToString("") { "%02x".format(it) }
        return AltinnAttachmentInitRequest(
            resourceId = RESOURCE_ID,
            fileName = "Tilsagnsbrev-${tilsagn.tiltakstype.navn}.pdf",
            displayName = vedleggNavn(tilsagn, personalia),
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
        correspondence = lagKorrespondanseBase(tilsagn, personalia),
        recipients = listOf(RECIPIENT_PREFIX + tilsagn.arrangor.organisasjonsnummer.value),
        existingAttachments = listOf(vedleggId),
        idempotentKey = idempotentKey,
    )

    private fun lagKorrespondanseBase(tilsagn: Tilsagn, personalia: Personalia): AltinnCorrespondenceBase {
        val requestedPublishTime = OffsetDateTime.now(ZoneId.systemDefault())
        val allowSystemDeleteAfter = requestedPublishTime.plusYears(10)

        return AltinnCorrespondenceBase(
            resourceId = RESOURCE_ID,
            sendersReference = tilsagn.bestilling.bestillingsnummer,
            messageSender = MSG_SENDER,
            content = lagInnhold(tilsagn, personalia),
            requestedPublishTime = requestedPublishTime,
            notification = lagVarsling(tilsagn),
            allowSystemDeleteAfter = allowSystemDeleteAfter,
        )
    }

    private fun lagInnhold(tilsagn: Tilsagn, personalia: Personalia) = AltinnCorrespondenceContent(
        language = LANGUAGE_CODE,
        messageTitle = vedleggNavn(tilsagn, personalia),
        messageBody = "Se vedlagt tilsagnsbrev for ${tilsagn.gjennomforing.navn}.",
    )

    private fun lagVarsling(tilsagn: Tilsagn): AltinnCorrespondenceNotification {
        val emne = "Tilsagnsbrev for ${tilsagn.arrangor.organisasjonsnummer.value} ${tilsagn.arrangor.navn} er tilgjengelig i Altinn"

        return AltinnCorrespondenceNotification(
            notificationTemplate = NOTIFICATION_TEMPLATE,
            notificationChannel = NOTIFICATION_CHANNEL,
            emailSubject = emne,
            emailBody = lagEpostTekst(tilsagn),
            sendReminder = false,
        )
    }

    private fun lagEpostTekst(tilsagn: Tilsagn): String = "Tilsagnsbrev for ${tilsagn.arrangor.organisasjonsnummer.value} ${tilsagn.arrangor.navn} er tilgjengelig. " +
        "Logg inn i Altinn for å se innholdet.\n\nVennlig hilsen Nav"

    private fun vedleggNavn(tilsagn: Tilsagn, personalia: Personalia): String = "Tilsagnsbrev ${tilsagn.tiltakstype.navn} ${personalia.navn()}"
}
