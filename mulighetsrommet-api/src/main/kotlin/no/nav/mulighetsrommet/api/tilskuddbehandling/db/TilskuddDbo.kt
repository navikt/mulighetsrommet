package no.nav.mulighetsrommet.api.tilskuddbehandling.db

import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddOpplaeringType
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.ValutaBelop
import java.util.UUID

data class TilskuddDbo(
    val id: UUID,
    val tilskuddOpplaeringType: TilskuddOpplaeringType,
    val soknadBelop: Int,
    val vedtakResultat: VedtakResultat,
    val kommentarVedtaksbrev: String?,
    val utbetalingMottaker: String,
    val kid: Kid?,
    val valutaBelop: ValutaBelop?,
)
