package no.nav.mulighetsrommet.api.tilskuddbehandling.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
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
    val utbetalingMottaker: UtbetalingMottaker,
    val kommentarIntern: String?,
    val vedtakJournalpostId: String?,
) {
    fun toTilskuddVedtak(): TilskuddVedtak {
        return TilskuddVedtak(
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
            utbetalingMottaker = when (this.utbetalingMottaker) {
                is UtbetalingMottaker.Bruker -> TilskuddMottaker.BRUKER
                is UtbetalingMottaker.Arrangor -> TilskuddMottaker.ARRANGOR
            },
            kid = (utbetalingMottaker as? UtbetalingMottaker.Arrangor)?.kid,
            forskuddsUtbetalingTilBruker = (utbetalingMottaker as? UtbetalingMottaker.Bruker)?.forskuddsUtbetaling ?: false,
            kommentarIntern = this.kommentarIntern,
            utbetalingBelop = this.utbetalingMottaker.utbetalingBelop,
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @JsonClassDiscriminator("type")
    @Serializable
    sealed interface UtbetalingMottaker {
        val utbetalingBelop: ValutaBelop?

        @Serializable
        @SerialName("BRUKER")
        data class Bruker(
            val forskuddsUtbetaling: Boolean,
            override val utbetalingBelop: ValutaBelop?,
        ) : UtbetalingMottaker

        @Serializable
        @SerialName("ARRANGOR")
        data class Arrangor(
            val kid: Kid?,
            override val utbetalingBelop: ValutaBelop?,
        ) : UtbetalingMottaker
    }
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
