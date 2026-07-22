package no.nav.mulighetsrommet.admin.deltaker

import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.admin.QueryContext
import no.nav.mulighetsrommet.api.domain.deltaker.DeltakerForslag
import java.util.UUID

data class ReplikerDeltakerForslag(val id: UUID, val forslag: DeltakerForslag?)

sealed interface ReplikerDeltakerForslagResultat {
    data class Slettet(val gjennomforingId: UUID) : ReplikerDeltakerForslagResultat

    data class Lagret(val gjennomforingId: UUID) : ReplikerDeltakerForslagResultat

    data object IngenEndring : ReplikerDeltakerForslagResultat
}

class ReplikerDeltakerForslagUseCase(
    private val db: AdminDatabase,
) {
    fun execute(command: ReplikerDeltakerForslag): ReplikerDeltakerForslagResultat = db.transaction {
        val (id, forslag) = command

        if (forslag == null) {
            val eksisterende = repository.deltakerForslag.get(id)
                ?: return@transaction ReplikerDeltakerForslagResultat.IngenEndring

            return@transaction deleteForslag(id, eksisterende.deltakerId)
        }

        when (forslag.status) {
            DeltakerForslag.Status.VENTER_PA_SVAR -> {
                val deltaker = repository.deltaker.get(forslag.deltakerId)
                    ?: return@transaction ReplikerDeltakerForslagResultat.IngenEndring

                repository.deltakerForslag.save(forslag)
                ReplikerDeltakerForslagResultat.Lagret(deltaker.gjennomforingId)
            }

            DeltakerForslag.Status.GODKJENT,
            DeltakerForslag.Status.AVVIST,
            DeltakerForslag.Status.TILBAKEKALT,
            DeltakerForslag.Status.ERSTATTET,
            -> deleteForslag(forslag.id, forslag.deltakerId)
        }
    }

    private fun QueryContext.deleteForslag(forslagId: UUID, deltakerId: UUID): ReplikerDeltakerForslagResultat {
        repository.deltakerForslag.delete(forslagId)
        return repository.deltaker.get(deltakerId)?.gjennomforingId
            ?.let { ReplikerDeltakerForslagResultat.Slettet(it) }
            ?: ReplikerDeltakerForslagResultat.IngenEndring
    }
}
