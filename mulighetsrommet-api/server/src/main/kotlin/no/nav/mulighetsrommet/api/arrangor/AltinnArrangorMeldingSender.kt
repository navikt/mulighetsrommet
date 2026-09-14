package no.nav.mulighetsrommet.api.arrangor

import arrow.core.Either
import arrow.core.flatMap
import no.nav.mulighetsrommet.admin.arrangor.ArrangorMeldingSender
import no.nav.mulighetsrommet.admin.arrangor.MeldingError
import no.nav.mulighetsrommet.admin.arrangor.MeldingId
import no.nav.mulighetsrommet.admin.arrangor.TilsagnsbrevArrangorMelding
import no.nav.mulighetsrommet.altinn.AltinnAttachmentInitRequest
import no.nav.mulighetsrommet.altinn.AltinnCorrespondenceBase
import no.nav.mulighetsrommet.altinn.AltinnCorrespondenceClient
import no.nav.mulighetsrommet.altinn.AltinnCorrespondenceContent
import no.nav.mulighetsrommet.altinn.AltinnCorrespondenceNotification
import no.nav.mulighetsrommet.altinn.AltinnCorrespondenceRequest
import java.security.MessageDigest
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class AltinnArrangorMeldingSender(
    private val client: AltinnCorrespondenceClient,
) : ArrangorMeldingSender {
    /**
     * ResourceId registrert i Altinns for utsending av tilsagnsbrev fra Nav til tiltaksarrangører.
     * Deles med tiltak-tilsagnsbrev (https://github.com/navikt/tiltak-tilsagnsbrev).
     */
    private val resourceId = "nav_tiltak_tilskuddsbrev"

    private val languageCode = "nb"
    private val messageSender = "Nav"
    private val recipientPrefix = "urn:altinn:organization:identifier-no:"
    private val notificationTemplate = "CustomMessage"
    private val notificationChannel = "Email"

    override suspend fun send(melding: TilsagnsbrevArrangorMelding): Either<MeldingError, MeldingId> {
        val vedlegg = tilAttachmentInitRequest(melding)
        return client.sendVedlegg(vedlegg, melding.vedlegg.innhold)
            .mapLeft { error -> MeldingError(error.message) }
            .flatMap { vedleggId ->
                val korrespondanse = tilCorrespondenceRequest(melding, vedleggId)
                client.sendKorrespondanse(korrespondanse)
                    .mapLeft { error -> MeldingError(error.message) }
                    .map { correspondenceId -> MeldingId(correspondenceId) }
            }
    }

    private fun tilAttachmentInitRequest(melding: TilsagnsbrevArrangorMelding): AltinnAttachmentInitRequest {
        val checksum = MessageDigest.getInstance("MD5")
            .digest(melding.vedlegg.innhold)
            .joinToString("") { "%02x".format(it) }

        return AltinnAttachmentInitRequest(
            resourceId = resourceId,
            fileName = melding.vedlegg.navn,
            displayName = melding.innhold.tittel,
            isEncrypted = false,
            sendersReference = melding.sendersReferanse,
            checksum = checksum,
        )
    }

    private fun tilCorrespondenceRequest(
        melding: TilsagnsbrevArrangorMelding,
        vedleggId: UUID,
        now: Instant = Instant.now(),
    ): AltinnCorrespondenceRequest = AltinnCorrespondenceRequest(
        correspondence = createCorrespondenceBase(melding, now),
        recipients = listOf(recipientPrefix + melding.mottakerOrganisasjonsnummer.value),
        existingAttachments = listOf(vedleggId),
        idempotentKey = melding.idempotentKey,
    )

    private fun createCorrespondenceBase(
        melding: TilsagnsbrevArrangorMelding,
        requestedPublishTime: Instant,
    ): AltinnCorrespondenceBase {
        val allowSystemDeleteAfter = OffsetDateTime.ofInstant(requestedPublishTime, ZoneOffset.UTC)
            .plusYears(10)
            .toInstant()

        return AltinnCorrespondenceBase(
            resourceId = resourceId,
            sendersReference = melding.sendersReferanse,
            messageSender = messageSender,
            content = AltinnCorrespondenceContent(
                language = languageCode,
                messageTitle = melding.innhold.tittel,
                messageBody = melding.innhold.brevtekst,
            ),
            requestedPublishTime = requestedPublishTime,
            notification = AltinnCorrespondenceNotification(
                notificationTemplate = notificationTemplate,
                notificationChannel = notificationChannel,
                emailSubject = melding.varsel.emne,
                emailBody = melding.varsel.tekst,
                sendReminder = false,
            ),
            allowSystemDeleteAfter = allowSystemDeleteAfter,
        )
    }
}
