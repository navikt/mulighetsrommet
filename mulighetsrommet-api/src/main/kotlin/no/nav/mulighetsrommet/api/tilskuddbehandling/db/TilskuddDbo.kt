package no.nav.mulighetsrommet.api.tilskuddbehandling.db

import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.vedtak.Opplaeringtilskudd
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.ValutaBelop
import java.util.UUID

data class TilskuddDbo(
    val id: UUID,
    val tilskuddOpplaeringType: Opplaeringtilskudd.Kode,
    val soknadBelop: ValutaBelop,
    val vedtakResultat: VedtakResultat,
    val kommentarVedtaksbrev: String?,
    val utbetalingMottaker: TilskuddMottaker,
    val kid: Kid?,
    val utbetalingBelop: ValutaBelop?,
)

enum class TilskuddMottaker {
    BRUKER,
    ARRANGOR,
}
