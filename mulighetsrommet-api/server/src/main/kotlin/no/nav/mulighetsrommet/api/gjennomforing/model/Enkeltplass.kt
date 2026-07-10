package no.nav.mulighetsrommet.api.gjennomforing.model

import no.nav.mulighetsrommet.api.totrinnskontroll.model.Totrinnskontroll

data class Enkeltplass(
    val gjennomforing: GjennomforingEnkeltplass,
    // TODO: gjøre totrinnskontroll del av GjennomforingEnkeltplass i stedet?
    val okonomi: Totrinnskontroll?,
)
