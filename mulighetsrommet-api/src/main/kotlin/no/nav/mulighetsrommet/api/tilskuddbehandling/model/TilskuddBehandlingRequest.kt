package no.nav.mulighetsrommet.api.tilskuddbehandling.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

@Serializable
data class TilskuddBehandlingRequest(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    val soknadJournalpostId: String?,
    @Serializable(with = LocalDateSerializer::class)
    val soknadDato: LocalDate?,
    val periodeStart: String?,
    val periodeSlutt: String?,
    val kostnadssted: NavEnhetNummer?,
    val kommentarIntern: String?,
    val tilskudd: List<TilskuddRequest>,
) {
    @Serializable
    data class TilskuddRequest(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val tilskuddOpplaeringType: TilskuddOpplaeringType?,
        val soknadBelop: Int?,
        val vedtakResultat: VedtakResultat?,
        val kommentarVedtaksbrev: String?,
        val utbetalingMottaker: String?,
        val kidNummer: String?,
        val belop: Int?,
    )
}
