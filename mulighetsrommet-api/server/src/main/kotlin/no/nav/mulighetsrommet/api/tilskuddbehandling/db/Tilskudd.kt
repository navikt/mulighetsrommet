package no.nav.mulighetsrommet.api.tilskuddbehandling.db

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

@Serializable
data class Tilskudd(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val type: Opplaeringtilskudd,
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    val tilskuddsnummer: String,
    val vedtak: List<Vedtak>,
) {
    @Serializable
    data class Vedtak(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        @Serializable(with = UUIDSerializer::class)
        val behandlingId: UUID,
        val lopenummer: Int,
        val soknadJournalpostId: String,
        @Serializable(with = LocalDateSerializer::class)
        val soknadDato: LocalDate,
        val periode: Periode,
        val kostnadssted: NavEnhetNummer,
        val soknadBelop: ValutaBelop,
        val utbetalingBelop: ValutaBelop?,
        val vedtakResultat: VedtakResultat,
        val kommentarVedtaksbrev: String?,
        val utbetalingMottaker: TilskuddMottaker,
        val kid: Kid?,
        val kommentarIntern: String?,
        val vedtakJournalpostId: String?,
    )
}

@Serializable
data class TilskuddKompakt(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val type: Opplaeringtilskudd,
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    val tilskuddsnummer: String,
    val sisteVedtakResultat: VedtakResultat?,
    val periode: Periode?,
    val sisteVedtakLopenummer: Int?,
)
