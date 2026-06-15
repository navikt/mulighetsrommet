package no.nav.mulighetsrommet.api.arrangorflate.db

import kotliquery.Session

class ArrangorflateQueries(val session: Session) {
    val tiltak = ArrangorflateTiltakQueries(session)
    val tilsagn = ArrangorflateTilsagnQueries(session)
    val utbetaling = ArrangorflateUtbetalingQueries(session)
}
