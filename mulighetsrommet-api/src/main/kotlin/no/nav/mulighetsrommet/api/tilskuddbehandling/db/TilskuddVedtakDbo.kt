package no.nav.mulighetsrommet.api.tilskuddbehandling.db

import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddOpplaeringType
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Valuta
import java.util.UUID

data class TilskuddVedtakDbo(
    val id: UUID,
    val tilskuddOpplaeringType: TilskuddOpplaeringType,
    val soknadBelop: Int,
    val soknadValuta: Valuta,
    val vedtakResultat: VedtakResultat,
    val kommentarVedtaksbrev: String?,
    val utbetalingMottaker: String,
    val kid: Kid?,
)
