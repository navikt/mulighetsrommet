package no.nav.mulighetsrommet.admin.arrangor

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.admin.enhetsregister.EnhetsregisterError
import no.nav.mulighetsrommet.admin.enhetsregister.EnhetsregisterGateway
import no.nav.mulighetsrommet.admin.enhetsregister.Hovedenhet
import no.nav.mulighetsrommet.admin.enhetsregister.Underenhet
import no.nav.mulighetsrommet.admin.enhetsregister.Virksomhet
import no.nav.mulighetsrommet.admin.enhetsregister.VirksomhetOppslag
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorKontaktperson
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate
import java.util.UUID

sealed interface SyncArrangorError {
    data class Enhetsregister(
        val error: EnhetsregisterError,
    ) : SyncArrangorError

    data class FjernetAvJuridiskeArsaker(
        val organisasjonsnummer: Organisasjonsnummer,
        val slettetDato: LocalDate,
    ) : SyncArrangorError
}

data class SyncArrangor(val organisasjonsnummer: Organisasjonsnummer)

data class SyncArrangorIfMissing(val organisasjonsnummer: Organisasjonsnummer)

data class DeleteArrangor(val organisasjonsnummer: Organisasjonsnummer)

class SyncArrangorUseCase(
    private val db: AdminDatabase,
    private val enhetsregister: EnhetsregisterGateway,
) {
    suspend fun execute(command: SyncArrangorIfMissing): Either<SyncArrangorError, Arrangor> {
        val arrangor = db.session { repository.arrangor.getByOrganisasjonsnummer(command.organisasjonsnummer) }
        return arrangor?.right() ?: execute(SyncArrangor(command.organisasjonsnummer))
    }

    suspend fun execute(command: SyncArrangor): Either<SyncArrangorError, Arrangor> {
        return enhetsregister.hentVirksomhet(command.organisasjonsnummer)
            .mapLeft { SyncArrangorError.Enhetsregister(it) }
            .flatMap { oppslag ->
                when (oppslag) {
                    is VirksomhetOppslag.FjernetAvJuridiskeArsaker -> {
                        markerSlettet(oppslag.organisasjonsnummer, oppslag.slettetDato)
                        SyncArrangorError
                            .FjernetAvJuridiskeArsaker(oppslag.organisasjonsnummer, oppslag.slettetDato)
                            .left()
                    }

                    is VirksomhetOppslag.Funnet -> syncVirksomhet(oppslag.virksomhet)
                }
            }
    }

    fun execute(command: DeleteArrangor): Unit = db.transaction {
        repository.arrangor.delete(command.organisasjonsnummer)
    }

    private suspend fun syncVirksomhet(virksomhet: Virksomhet): Either<SyncArrangorError, Arrangor> {
        // Sørger for at hovedenheten også finnes lagret før underenheten materialiseres
        val overordnetEnhet = (virksomhet as? Underenhet)
            ?.takeIf { it.slettetDato == null }
            ?.overordnetEnhet

        return if (overordnetEnhet != null) {
            execute(SyncArrangorIfMissing(overordnetEnhet)).map { save(virksomhet) }
        } else {
            save(virksomhet).right()
        }
    }

    private fun save(virksomhet: Virksomhet): Arrangor = db.transaction {
        val eksisterende = repository.arrangor.getByOrganisasjonsnummer(virksomhet.organisasjonsnummer)
        val id = eksisterende?.id ?: UUID.randomUUID()
        val kontaktpersoner = eksisterende?.kontaktpersoner ?: listOf()
        val arrangor = virksomhet.toArrangor(id, kontaktpersoner)
        repository.arrangor.save(arrangor)
        arrangor
    }

    private fun markerSlettet(orgnr: Organisasjonsnummer, slettetDato: LocalDate): Unit = db.transaction {
        when (val arrangor = repository.arrangor.getByOrganisasjonsnummer(orgnr)) {
            is Arrangor.Norsk -> repository.arrangor.save(arrangor.copy(slettetDato = slettetDato))
            is Arrangor.Utenlandsk -> repository.arrangor.save(arrangor.copy(slettetDato = slettetDato))
            null -> {}
        }
    }
}

private fun Virksomhet.toArrangor(id: UUID, kontaktpersoner: List<ArrangorKontaktperson>) = when (this) {
    is Hovedenhet -> Arrangor.Norsk(
        id = id,
        organisasjonsnummer = organisasjonsnummer,
        organisasjonsform = organisasjonsform,
        navn = navn,
        overordnetEnhet = null,
        slettetDato = slettetDato,
        kontaktpersoner = kontaktpersoner,
    )

    is Underenhet -> Arrangor.Norsk(
        id = id,
        organisasjonsnummer = organisasjonsnummer,
        organisasjonsform = organisasjonsform,
        navn = navn,
        overordnetEnhet = if (slettetDato == null) overordnetEnhet else null,
        slettetDato = slettetDato,
        kontaktpersoner = kontaktpersoner,
    )
}
