package no.nav.mulighetsrommet.api.tilskuddbehandling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.util.UUID

@Serializable
data class TilskuddOpplaeringDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tilskuddOpplaeringType: TilskuddOpplaeringType,
    val soknadBelop: ValutaBelop,
    val vedtakResultat: VedtakResultatDto,
    val kommentarVedtaksbrev: String?,
    val utbetalingMottaker: String,
    val kid: Kid?,
    val utbetalingBelop: ValutaBelop?,
)

@Serializable
data class VedtakResultatDto(
    val type: VedtakResultat,
) {
    val status: DataElement.Status = toVedtakResultatStatus(type)
}

fun toVedtakResultatStatus(vedtakResultat: VedtakResultat): DataElement.Status {
    return when (vedtakResultat) {
        VedtakResultat.INNVILGELSE -> DataElement.Status(vedtakResultat.beskrivelse, DataElement.Status.Variant.SUCCESS)
        VedtakResultat.AVSLAG -> DataElement.Status(vedtakResultat.beskrivelse, DataElement.Status.Variant.ERROR)
    }
}
