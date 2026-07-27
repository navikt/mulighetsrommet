package no.nav.mulighetsrommet.api.utbetaling.model

import arrow.core.Either
import no.nav.mulighetsrommet.model.FieldError

/**
 * Ved unntakstilfeller så kan operasjoner (opprettelse, godkjenning etc) på utbetalinger feile
 * pga. uventede valideringsfeil. Årsaker kan f.eks. være samtidighetsproblemer, glemte
 * preconditions/låser eller andre bugs/mangler i koden.
 *
 * Disse blir kastet som exceptions i stedet for returneres som en [Either.Left] fordi det integrerer
 * bedre med rollback av database-transaksjoner.
 */
class UtbetalingException(val errors: List<FieldError>) : Exception("UtbetalingException(errors=$errors)")
