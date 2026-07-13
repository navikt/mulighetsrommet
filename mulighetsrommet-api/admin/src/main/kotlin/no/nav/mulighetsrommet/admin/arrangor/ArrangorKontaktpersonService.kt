package no.nav.mulighetsrommet.admin.arrangor

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorKontaktperson
import java.util.UUID

sealed interface ArrangorKontaktpersonError {
    data object KontaktpersonErIBruk : ArrangorKontaktpersonError
}

@Serializable
data class KoblingerForKontaktperson(
    val gjennomforinger: List<DokumentKoblingForKontaktperson>,
    val avtaler: List<DokumentKoblingForKontaktperson>,
)

class ArrangorKontaktpersonService(
    private val db: AdminDatabase,
) {
    fun upsert(kontaktperson: ArrangorKontaktperson): Unit = db.transaction {
        val arrangor = repository.arrangor.get(kontaktperson.arrangorId)
        val kontaktpersoner = arrangor.kontaktpersoner.filterNot { it.id == kontaktperson.id } + kontaktperson
        repository.arrangor.save(arrangor.medKontaktpersoner(kontaktpersoner))
    }

    fun delete(arrangorId: UUID, kontaktpersonId: UUID): Either<ArrangorKontaktpersonError, Unit> = db.transaction {
        val arrangor = repository.arrangor.get(arrangorId)

        val (gjennomforinger, avtaler) = queries.arrangor.koblingerTilKontaktperson(kontaktpersonId)
        if (gjennomforinger.isNotEmpty() || avtaler.isNotEmpty()) {
            return@transaction ArrangorKontaktpersonError.KontaktpersonErIBruk.left()
        }

        val kontaktpersoner = arrangor.kontaktpersoner.filterNot { it.id == kontaktpersonId }
        repository.arrangor.save(arrangor.medKontaktpersoner(kontaktpersoner))

        Unit.right()
    }

    fun hentAlle(arrangorId: UUID): List<ArrangorKontaktperson> = db.session {
        queries.arrangor.getKontaktpersoner(arrangorId)
    }

    fun hentKoblinger(kontaktpersonId: UUID): KoblingerForKontaktperson = db.session {
        val (gjennomforinger, avtaler) = queries.arrangor.koblingerTilKontaktperson(kontaktpersonId)
        KoblingerForKontaktperson(
            gjennomforinger = gjennomforinger,
            avtaler = avtaler,
        )
    }
}
