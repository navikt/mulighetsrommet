package no.nav.mulighetsrommet.admin.journalpost

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.api.clients.saf.SafBruker
import no.nav.mulighetsrommet.api.clients.saf.SafBrukerIdType
import no.nav.mulighetsrommet.api.clients.saf.SafClient
import no.nav.mulighetsrommet.api.clients.saf.SafError
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.tokenprovider.AccessType

object JournalpostValidator {
    suspend fun validerJournalpost(
        journalpostId: String,
        forventetBruker: ForventetBruker,
        accessType: AccessType,
        safClient: SafClient,
    ): Either<JournalpostValideringError, JournalpostId> {
        val validatedId = JournalpostId.parse(journalpostId)
            ?: return JournalpostValideringError.FeilFormat.left()

        return safClient.hentJournalpost(journalpostId, accessType)
            .mapLeft {
                when (it) {
                    SafError.NotFound -> JournalpostValideringError.NotFound
                    SafError.Error -> JournalpostValideringError.SafError
                }
            }
            .flatMap { journalpost ->
                validerBruker(journalpost.bruker, forventetBruker)
            }
            .map { validatedId }
    }

    private fun validerBruker(
        bruker: SafBruker?,
        forventetBruker: ForventetBruker,
    ): Either<JournalpostValideringError, Unit> = when (bruker?.type) {
        // TODO: Ikke helt sikker på at dette alltid skal være en feil, men vi kan vel begynne med det
        null -> JournalpostValideringError.IngenTilknytning.left()

        SafBrukerIdType.FNR ->
            when (forventetBruker) {
                is ForventetBruker.Bedrift -> JournalpostValideringError.TilhorerPerson.left()

                is ForventetBruker.Person -> {
                    if (bruker.id == forventetBruker.norskIdent.value) {
                        Unit.right()
                    } else {
                        JournalpostValideringError.TilhorerAnnenPerson.left()
                    }
                }
            }

        SafBrukerIdType.ORGNR ->
            when (forventetBruker) {
                is ForventetBruker.Bedrift -> {
                    if (bruker.id == forventetBruker.organisasjonsnummer.value) {
                        Unit.right()
                    } else {
                        JournalpostValideringError.TilhorerAnnenVirksomhet.left()
                    }
                }

                is ForventetBruker.Person ->
                    JournalpostValideringError.TilhorerVirksomhet.left()
            }

        // Jeg tror ikke denne inntreffer, hvis den gjør det må vi veksle aktørId -> fnr i pdl først
        SafBrukerIdType.AKTOERID ->
            JournalpostValideringError.AktoerId.left()
    }

    enum class JournalpostValideringError {
        NotFound,
        SafError,
        FeilFormat,
        TilhorerVirksomhet,
        TilhorerPerson,
        TilhorerAnnenPerson,
        TilhorerAnnenVirksomhet,
        AktoerId,
        IngenTilknytning,
    }
}

sealed class ForventetBruker {
    data class Person(val norskIdent: NorskIdent) : ForventetBruker()
    data class Bedrift(val organisasjonsnummer: Organisasjonsnummer) : ForventetBruker()
}

/*
                pointer,

 */
