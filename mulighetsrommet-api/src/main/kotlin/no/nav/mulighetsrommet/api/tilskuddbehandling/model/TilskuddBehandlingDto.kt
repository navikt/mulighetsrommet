package no.nav.mulighetsrommet.api.tilskuddbehandling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.DataElement
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

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
    val kostnadssted: NavEnhetNummer,
    val vedtak: List<TilskuddVedtakDto>,
    val status: TilskuddBehandlingStatusDto,
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
