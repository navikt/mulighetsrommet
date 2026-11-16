package no.nav.tiltak.historikk.replikering

import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.model.KometDeltaker
import org.slf4j.LoggerFactory
import java.util.UUID

class KometDeltakerReplikering(
    private val db: TiltakshistorikkDatabase,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(input: KometDeltaker) {
        val deltaker = input.copy(
            statusOpprettetTidspunkt = input.statusOpprettetTidspunkt.truncatedToMicros(),
            opprettetTidspunkt = input.opprettetTidspunkt.truncatedToMicros(),
            oppdatertTidspunkt = input.oppdatertTidspunkt.truncatedToMicros(),
        )
        val id = deltaker.id
        if (deltaker.statusType == DeltakerStatusType.FEILREGISTRERT) {
            logger.info("Sletter deltaker med id=$id fordi den var feilregistrert")
            delete(id)
            return
        }

        db.session {
            val stored = queries.kometDeltaker.get(id)

            if (stored != null && deltaker.oppdatertTidspunkt < stored.oppdatertTidspunkt) {
                logger.info("Hopper over deltaker med id=$id fordi innkommende oppdatertTidspunkt er eldre enn lagret")
                return@session
            }

            logger.info("Forsøker å lagre deltaker med id=$id")
            queries.kometDeltaker.upsertKometDeltaker(deltaker)
        }
    }

    fun delete(id: UUID): Unit = db.session {
        queries.kometDeltaker.deleteKometDeltaker(id)
    }
}
