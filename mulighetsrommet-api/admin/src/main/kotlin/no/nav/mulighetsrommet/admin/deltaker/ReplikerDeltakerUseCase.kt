package no.nav.mulighetsrommet.admin.deltaker

import no.nav.mulighetsrommet.admin.AdminDatabase
import no.nav.mulighetsrommet.admin.QueryContext
import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import org.slf4j.LoggerFactory
import java.util.UUID

data class ReplikerDeltaker(val id: UUID, val deltaker: Deltaker?)

sealed interface ReplikerDeltakerResultat {
    data class Slettet(val gjennomforingId: UUID) : ReplikerDeltakerResultat

    data class Lagret(val gjennomforingId: UUID) : ReplikerDeltakerResultat

    data object IngenEndring : ReplikerDeltakerResultat
}

class ReplikerDeltakerUseCase(
    private val db: AdminDatabase,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(command: ReplikerDeltaker): ReplikerDeltakerResultat = db.transaction {
        val (id, deltaker) = command

        if (deltaker == null) {
            val gjennomforingId = repository.deltaker.get(id)?.gjennomforingId
                ?: return@transaction ReplikerDeltakerResultat.IngenEndring

            logger.info("Sletter deltaker id=$id for gjennomforingId=$gjennomforingId")
            repository.deltaker.delete(id)
            return@transaction ReplikerDeltakerResultat.Slettet(gjennomforingId)
        }

        if (deltaker.erFeilregistrert()) {
            logger.info("Sletter feilregistrert deltaker id=$id for gjennomforingId=${deltaker.gjennomforingId}")
            repository.deltaker.delete(id)
            return@transaction ReplikerDeltakerResultat.Slettet(deltaker.gjennomforingId)
        }

        if (!harEndringer(deltaker)) {
            return@transaction ReplikerDeltakerResultat.IngenEndring
        }

        repository.deltaker.save(deltaker)
        ReplikerDeltakerResultat.Lagret(deltaker.gjennomforingId)
    }

    private fun QueryContext.harEndringer(nyDeltaker: Deltaker): Boolean {
        val eksisterende = repository.deltaker.get(nyDeltaker.id) ?: return true

        if (nyDeltaker.endretTidspunkt < eksisterende.endretTidspunkt) {
            return false
        }

        return eksisterende != nyDeltaker
    }
}
