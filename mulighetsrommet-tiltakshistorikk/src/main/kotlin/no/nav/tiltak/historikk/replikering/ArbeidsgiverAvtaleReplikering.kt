package no.nav.tiltak.historikk.replikering

import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.model.ArbeidsgiverAvtale
import org.slf4j.LoggerFactory
import java.util.UUID

class ArbeidsgiverAvtaleReplikering(
    private val db: TiltakshistorikkDatabase,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(input: ArbeidsgiverAvtale): Unit = db.session {
        val avtale = input.copy(
            opprettetTidspunkt = input.opprettetTidspunkt.truncatedToMicros(),
            oppdatertTidspunkt = input.oppdatertTidspunkt.truncatedToMicros(),
        )
        val id = avtale.avtaleId
        val stored = queries.arbeidsgiverAvtale.get(id)

        if (stored != null && avtale.oppdatertTidspunkt < stored.oppdatertTidspunkt) {
            logger.info("Hopper over avtale med id=$id fordi innkommende oppdatertTidspunkt er eldre enn lagret")
            return@session
        }

        queries.arbeidsgiverAvtale.upsert(avtale)
    }

    fun delete(id: UUID): Unit = db.session {
        queries.arbeidsgiverAvtale.delete(id)
    }
}
