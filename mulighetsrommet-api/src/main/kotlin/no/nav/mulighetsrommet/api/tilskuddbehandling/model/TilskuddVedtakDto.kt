package no.nav.mulighetsrommet.api.tilskuddbehandling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class TilskuddVedtakDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tilskuddType: TilskuddType,
    val soknadBelop: Int,
    val soknadValuta: Valuta,
    val vedtakResultat: VedtakResultat,
    val kommentarVedtaksbrev: String?,
    val utbetalingMottaker: String,
)
