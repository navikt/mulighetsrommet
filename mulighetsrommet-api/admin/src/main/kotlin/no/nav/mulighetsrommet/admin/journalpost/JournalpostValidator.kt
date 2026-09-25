package no.nav.mulighetsrommet.admin.journalpost

import no.nav.mulighetsrommet.api.clients.saf.SafClient
import no.nav.mulighetsrommet.api.clients.saf.SafError
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.validation.Validated

/**
 * Validerer at en oppgitt journalpostId finnes i SAF (Sak og arkiv).
 *
 * Sjekker kun at journalposten finnes, ikke at den tilhører en bestemt person.
 */
class JournalpostValidator(
    private val saf: SafClient,
) {
    suspend fun validerJournalpostFinnes(
        journalpostId: String,
        pointer: String,
        accessType: AccessType,
    ): Validated<Unit> = saf.hentJournalpost(journalpostId, accessType)
        .map { }
        .mapLeft {
            when (it) {
                SafError.NotFound -> listOf(
                    FieldError(pointer, "Fant ingen journalpost med id $journalpostId"),
                )

                SafError.Error -> listOf(
                    FieldError(pointer, "Klarte ikke å slå opp journalpost. Prøv igjen senere."),
                )
            }
        }
}
