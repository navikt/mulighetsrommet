package no.nav.mulighetsrommet.admin.deltaker

import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.admin.QueryContext
import no.nav.mulighetsrommet.api.domain.deltaker.DeltakerForslag
import java.util.UUID

data class ReplikerDeltakerForslag(
    val id: UUID,
    val deltakerId: UUID,
    val endring: DeltakerForslag.Endring,
    val status: DeltakerForslag.Status,
)

data class SlettDeltakerForslag(
    val id: UUID,
)

sealed interface ReplikerDeltakerForslagResultat {
    data class Slettet(val gjennomforingId: UUID) : ReplikerDeltakerForslagResultat

    data class Lagret(val gjennomforingId: UUID) : ReplikerDeltakerForslagResultat

    data object IngenEndring : ReplikerDeltakerForslagResultat
}

class ReplikerDeltakerForslagUseCase(
    private val db: AdminDatabase,
) {
    fun execute(command: ReplikerDeltakerForslag): ReplikerDeltakerForslagResultat = db.transaction {
        when (command.status) {
            DeltakerForslag.Status.VENTER_PA_SVAR -> {
                val deltaker = repository.deltaker.get(command.deltakerId)
                    ?: return@transaction ReplikerDeltakerForslagResultat.IngenEndring

                val nyttForslag = DeltakerForslag.fraDeltaker(deltaker, command.id, command.endring, command.status)
                repository.deltakerForslag.save(nyttForslag)
                ReplikerDeltakerForslagResultat.Lagret(deltaker.gjennomforingId)
            }

            DeltakerForslag.Status.GODKJENT,
            DeltakerForslag.Status.AVVIST,
            DeltakerForslag.Status.TILBAKEKALT,
            DeltakerForslag.Status.ERSTATTET,
            -> deleteForslag(command.id)
        }
    }

    fun execute(command: SlettDeltakerForslag): ReplikerDeltakerForslagResultat = db.transaction {
        deleteForslag(command.id)
    }

    private fun QueryContext.deleteForslag(forslagId: UUID): ReplikerDeltakerForslagResultat {
        val eksisterende = repository.deltakerForslag.get(forslagId)
            ?: return ReplikerDeltakerForslagResultat.IngenEndring

        repository.deltakerForslag.delete(forslagId)
        return ReplikerDeltakerForslagResultat.Slettet(eksisterende.gjennomforingId)
    }
}
