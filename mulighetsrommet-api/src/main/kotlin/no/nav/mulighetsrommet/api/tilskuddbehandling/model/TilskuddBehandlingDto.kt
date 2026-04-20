package no.nav.mulighetsrommet.api.tilskuddbehandling.model

import kotlinx.serialization.Serializable
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
    val status: TilskuddBehandlingStatus,
)
