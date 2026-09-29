package no.nav.mulighetsrommet.api.tilskuddbehandling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.tilsagn.api.KostnadsstedDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottaker
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddVedtak
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

@Serializable
data class TilskuddOpplaeringDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tilskuddsnummer: String,
    @Serializable(with = UUIDSerializer::class)
    val tilskuddId: UUID,
    val tilskuddOpplaeringType: Opplaeringtilskudd.Kode,
    @Serializable(with = LocalDateSerializer::class)
    val soknadDato: LocalDate,
    val soknadJournalpostId: JournalpostId,
    val soknadBelop: ValutaBelop,
    val periode: Periode,
    val kostnadssted: KostnadsstedDto,
    val vedtakResultat: VedtakResultatDto,
    val kommentarVedtaksbrev: String?,
    val utbetalingMottaker: TilskuddMottaker,
    val kid: Kid?,
    val utbetalingBelop: ValutaBelop?,
    val kommentarIntern: String?,
    val vedtakJournalpostId: String?,
) {
    fun toTilskuddVedtak() = TilskuddVedtak(
        id = this.id,
        tilskuddId = this.tilskuddId,
        tilskuddOpplaeringType = this.tilskuddOpplaeringType,
        soknadDato = this.soknadDato,
        soknadJournalpostId = this.soknadJournalpostId,
        periode = this.periode,
        soknadBelop = this.soknadBelop,
        kostnadssted = this.kostnadssted.enhetsnummer,
        vedtakResultat = this.vedtakResultat.type,
        kommentarVedtaksbrev = this.kommentarVedtaksbrev,
        utbetalingMottaker = this.utbetalingMottaker,
        kid = this.kid,
        kommentarIntern = this.kommentarIntern,
        utbetalingBelop = this.utbetalingBelop,
    )
}

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
