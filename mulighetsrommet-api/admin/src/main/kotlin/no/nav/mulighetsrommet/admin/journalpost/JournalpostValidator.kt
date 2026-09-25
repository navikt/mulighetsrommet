package no.nav.mulighetsrommet.admin.journalpost

import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.api.clients.saf.SafBruker
import no.nav.mulighetsrommet.api.clients.saf.SafBrukerIdType
import no.nav.mulighetsrommet.api.clients.saf.SafClient
import no.nav.mulighetsrommet.api.clients.saf.SafError
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.tokenprovider.AccessType
import no.nav.mulighetsrommet.validation.Validated

/**
 * Validerer en oppgitt journalpostId mot SAF (Sak og arkiv).
 *
 * Sjekker at journalposten finnes, og at parten den gjelder stemmer med forventet identitet:
 * er journalposten knyttet til en person må det være riktig person, er den knyttet til en
 * virksomhet må det være riktig virksomhet.
 */
class JournalpostValidator(
    private val saf: SafClient,
) {
    /**
     * @param forventetBruker fødselsnummeret journalposten skal tilhøre dersom den er knyttet til en person.
     * @param forventetArrangor organisasjonsnummeret journalposten skal tilhøre dersom den er knyttet til en virksomhet.
     */
    suspend fun validerJournalpost(
        journalpostId: String,
        forventetBruker: NorskIdent?,
        forventetArrangor: Organisasjonsnummer?,
        pointer: String,
        accessType: AccessType,
    ): Validated<Unit> = saf.hentJournalpost(journalpostId, accessType)
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
        .flatMap { journalpost ->
            validerBruker(journalpost.bruker, forventetBruker, forventetArrangor, pointer)
        }

    private fun validerBruker(
        bruker: SafBruker?,
        forventetBruker: NorskIdent?,
        forventetArrangor: Organisasjonsnummer?,
        pointer: String,
    ): Validated<Unit> = when (bruker?.type) {
        // Journalposten er ikke knyttet til en part i SAF. Da har vi ingenting å verifisere mot.
        null -> Unit.right()

        SafBrukerIdType.FNR ->
            if (bruker.id != null && bruker.id == forventetBruker?.value) {
                Unit.right()
            } else {
                listOf(FieldError(pointer, "Journalposten tilhører en annen person enn deltakeren")).left()
            }

        SafBrukerIdType.ORGNR ->
            if (bruker.id != null && bruker.id == forventetArrangor?.value) {
                Unit.right()
            } else {
                listOf(FieldError(pointer, "Journalposten tilhører en annen virksomhet enn arrangøren")).left()
            }

        // Vi har fødselsnummer, ikke aktørid, og kan derfor ikke sammenligne mot en aktørid.
        SafBrukerIdType.AKTOERID ->
            listOf(FieldError(pointer, "Kunne ikke verifisere hvem journalposten tilhører")).left()
    }
}
