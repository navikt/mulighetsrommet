package no.nav.mulighetsrommet.api.utbetaling.tilskudd.service

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.utbetaling.tilskudd.model.OpprettTilskuddUtbetaling
import no.nav.mulighetsrommet.model.Agent

class TilskuddUtbetalingService(
    private val config: Config,
    private val db: ApiDatabase,
) {

    data class Config(
        val utbetatlingTilskuddTopic: String,
    )

    suspend fun opprettUtbetaling(
        opprett: OpprettTilskuddUtbetaling,
        agent: Agent,
    ) = db.transaction {
        val gjennomforing = queries.gjennomforing.getGjennomforingEnkeltplassOrError(opprett.gjennomforingId)
    }
}
