package no.nav.mulighetsrommet.api.tilskuddbehandling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.admin.totrinnskontroll.TotrinnskontrollDto
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.tilsagn.api.KostnadsstedDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandling
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddDbo
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID
import kotlin.String

@Serializable
data class TilskuddBehandlingDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    val soknadJournalpostId: String,
    @Serializable(with = LocalDateSerializer::class)
    val soknadDato: LocalDate,
    val periode: Periode,
    val kostnadssted: KostnadsstedDto,
    val tilskudd: List<TilskuddOpplaeringDto>,
    val status: TilskuddBehandlingStatusDto,
    val kommentarIntern: String?,
    val samletVedtakResultat: SamletVedtakResultat,
    val vedtakJournalpostId: String?,
) {
    fun toDbo() = TilskuddBehandling(
        id = this.id,
        gjennomforingId = this.gjennomforingId,
        soknadJournalpostId = this.soknadJournalpostId,
        soknadDato = this.soknadDato,
        periode = this.periode,
        tilskudd = this.tilskudd.map {
            TilskuddDbo(
                id = it.id,
                tilskuddOpplaeringType = it.tilskuddOpplaeringType,
                soknadBelop = it.soknadBelop,
                vedtakResultat = it.vedtakResultat.type,
                kommentarVedtaksbrev = it.kommentarVedtaksbrev,
                utbetalingMottaker = it.utbetalingMottaker,
                kid = it.kid,
                utbetalingBelop = it.utbetalingBelop,
            )
        },
        kostnadssted = this.kostnadssted.enhetsnummer,
        status = this.status.type,
        kommentarIntern = this.kommentarIntern,
    )
}

@Serializable
data class TilskuddBehandlingDetaljerDto(
    val behandling: TilskuddBehandlingDto,
    val opprettelse: TotrinnskontrollDto,
    val handlinger: Set<TilskuddBehandlingHandling>,
)

@Serializable
data class TilskuddBehandlingKompakt(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = LocalDateSerializer::class)
    val soknadDato: LocalDate,
    val journalpostId: String,
    val tilskuddtyper: Set<Opplaeringtilskudd.Kode>,
    val periode: Periode,
    val kostnadssted: KostnadsstedDto,
    val status: TilskuddBehandlingStatusDto,
    val samletVedtakResultat: SamletVedtakResultat,
)

@Serializable
data class TilskuddBehandlingStatusDto(
    val type: TilskuddBehandlingStatus,
) {
    val status: DataElement.Status = toTilskuddBehandlingStatusTag(type)
}

fun toTilskuddBehandlingStatusTag(status: TilskuddBehandlingStatus): DataElement.Status {
    val variant = when (status) {
        TilskuddBehandlingStatus.TIL_ATTESTERING -> DataElement.Status.Variant.INFO
        TilskuddBehandlingStatus.FERDIG_BEHANDLET -> DataElement.Status.Variant.SUCCESS
        TilskuddBehandlingStatus.RETURNERT -> DataElement.Status.Variant.ERROR
    }
    return DataElement.Status(status.beskrivelse, variant)
}

fun samletVedtakResultatStatusTag(vedtakResultat: List<VedtakResultat>): SamletVedtakResultat {
    return if (vedtakResultat.all { it == VedtakResultat.INNVILGELSE }) {
        SamletVedtakResultat.INNVILGELSE
    } else if (vedtakResultat.all { it == VedtakResultat.AVSLAG }) {
        SamletVedtakResultat.AVSLAG
    } else {
        SamletVedtakResultat.DELVIS_INNVILGELSE
    }
}

@Serializable
enum class TilskuddBehandlingHandling {
    REDIGER,
    ATTESTER,
    RETURNER,
}
