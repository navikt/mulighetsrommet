package no.nav.mulighetsrommet.api.gjennomforing.model

import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll

data class Enkeltplass(
    val gjennomforing: GjennomforingEnkeltplass,
    val okonomi: Totrinnskontroll?,
    val prisendring: Prisendring? = null,
) {
    data class Prisendring(
        val prismodell: Prismodell,
        val totrinnskontroll: Totrinnskontroll,
    )
}
