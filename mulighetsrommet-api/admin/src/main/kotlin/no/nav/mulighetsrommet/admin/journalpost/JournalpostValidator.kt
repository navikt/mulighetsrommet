package no.nav.mulighetsrommet.admin.journalpost

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.api.clients.pdl.GraphqlRequest
import no.nav.mulighetsrommet.api.clients.pdl.HentHistoriskeIdenterPdlQuery
import no.nav.mulighetsrommet.api.clients.pdl.IdentGruppe
import no.nav.mulighetsrommet.api.clients.pdl.PdlError
import no.nav.mulighetsrommet.api.clients.pdl.PdlIdent
import no.nav.mulighetsrommet.api.clients.saf.SafBruker
import no.nav.mulighetsrommet.api.clients.saf.SafBrukerIdType
import no.nav.mulighetsrommet.api.clients.saf.SafClient
import no.nav.mulighetsrommet.api.clients.saf.SafError
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.tokenprovider.AccessType

class JournalpostValidator(
    private val safClient: SafClient,
    private val hentIdenterQuery: HentHistoriskeIdenterPdlQuery,
) {
    suspend fun validerJournalpost(
        journalpostId: String,
        forventetBruker: ForventetBruker,
        accessType: AccessType,
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
                validerBruker(journalpost.bruker, forventetBruker, accessType)
            }
            .map { validatedId }
    }

    private suspend fun validerBruker(
        bruker: SafBruker?,
        forventetBruker: ForventetBruker,
        accessType: AccessType,
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

                is ForventetBruker.PersonEllerBedrift -> {
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

                is ForventetBruker.PersonEllerBedrift -> {
                    if (bruker.id == forventetBruker.organisasjonsnummer.value) {
                        Unit.right()
                    } else {
                        JournalpostValideringError.TilhorerAnnenVirksomhet.left()
                    }
                }

                is ForventetBruker.Person ->
                    JournalpostValideringError.TilhorerVirksomhet.left()
            }

        // Journalposten er knyttet til en aktørId. Vi veksler den til fnr i PDL og
        // sammenligner mot forventet bruker.
        SafBrukerIdType.AKTOERID ->
            when (forventetBruker) {
                is ForventetBruker.Bedrift -> JournalpostValideringError.TilhorerPerson.left()

                is ForventetBruker.Person ->
                    validerAktoerIdMotPerson(bruker.id, forventetBruker.norskIdent, accessType)

                is ForventetBruker.PersonEllerBedrift ->
                    validerAktoerIdMotPerson(bruker.id, forventetBruker.norskIdent, accessType)
            }
    }

    private suspend fun validerAktoerIdMotPerson(
        aktoerId: String?,
        forventetNorskIdent: NorskIdent,
        accessType: AccessType,
    ): Either<JournalpostValideringError, Unit> = hentIdenterQuery
        .hentHistoriskeIdenter(
            GraphqlRequest.HentHistoriskeIdenter(
                ident = PdlIdent(requireNotNull(aktoerId) { "aktoerId var null" }),
                grupper = listOf(IdentGruppe.FOLKEREGISTERIDENT),
            ),
            accessType,
        )
        .mapLeft {
            when (it) {
                PdlError.NotFound, PdlError.Error -> JournalpostValideringError.KunneIkkeVeksleAktoerId
            }
        }
        .flatMap { identer ->
            if (identer.any { it.ident.value == forventetNorskIdent.value }) {
                Unit.right()
            } else {
                JournalpostValideringError.TilhorerAnnenPerson.left()
            }
        }

    enum class JournalpostValideringError {
        NotFound,
        SafError,
        FeilFormat,
        TilhorerVirksomhet,
        TilhorerPerson,
        TilhorerAnnenPerson,
        TilhorerAnnenVirksomhet,
        KunneIkkeVeksleAktoerId,
        IngenTilknytning,
    }
}

sealed class ForventetBruker {
    data class Person(val norskIdent: NorskIdent) : ForventetBruker()
    data class Bedrift(val organisasjonsnummer: Organisasjonsnummer) : ForventetBruker()
    data class PersonEllerBedrift(
        val norskIdent: NorskIdent,
        val organisasjonsnummer: Organisasjonsnummer,
    ) : ForventetBruker()
}
