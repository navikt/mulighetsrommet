package no.nav.mulighetsrommet.api.utbetaling

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.UtbetalingDto
import java.util.*

fun ApiDatabase.getUtbetalingDto(id: UUID): UtbetalingDto? {
    return session {
        queries.utbetaling.get(id)
    }
}

fun UtbetalingDbo.toUtbetalingDto(db: ApiDatabase): UtbetalingDto {
    return db.session {
        queries.utbetaling.get(this@toUtbetalingDto.id)
            ?: throw IllegalStateException("Failed to get UtbetalingDto for id ${this@toUtbetalingDto.id}")
    }
}