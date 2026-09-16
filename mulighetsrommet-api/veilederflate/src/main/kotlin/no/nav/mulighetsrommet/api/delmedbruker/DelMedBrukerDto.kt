package no.nav.mulighetsrommet.api.delmedbruker

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class DelMedBrukerDto(
    val tiltak: Tiltak,
    val dialogId: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val tidspunkt: LocalDateTime,
    val tiltakstype: Tiltakstype,
) {
    @Serializable
    data class Tiltak(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID?,
        val navn: String?,
        val slettet: Boolean,
    )

    @Serializable
    data class Tiltakstype(
        val tiltakskode: Tiltakskode,
        val navn: String,
    )
}

data class DelMedBrukerDbo(
    val norskIdent: NorskIdent,
    val navIdent: NavIdent,
    val dialogId: String,
    val tiltakstypeId: UUID,
    val tiltakDokumentId: UUID?,
    val gjennomforingId: UUID?,
    val deltFraEnhet: NavEnhetNummer,
)
