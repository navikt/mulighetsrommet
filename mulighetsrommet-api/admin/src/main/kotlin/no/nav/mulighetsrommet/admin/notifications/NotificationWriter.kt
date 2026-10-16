package no.nav.mulighetsrommet.admin.notifications

import arrow.core.NonEmptyList
import no.nav.mulighetsrommet.model.NavIdent
import java.time.Instant

/**
 * Planlegger varsler til Nav-ansatte i samme transaksjon som endringen. Implementeres i persistence.
 */
interface NotificationWriter {
    fun schedule(title: String, targets: NonEmptyList<NavIdent>, createdAt: Instant)
}
