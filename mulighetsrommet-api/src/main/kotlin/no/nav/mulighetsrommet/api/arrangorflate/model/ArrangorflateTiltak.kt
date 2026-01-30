package no.nav.mulighetsrommet.api.arrangorflate.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

data class ArrangorflateTiltak(
    val id: UUID,
    val lopenummer: Tiltaksnummer,
    val navn: String,
    val tiltakstype: Tiltakstype,
    val arrangor: ArrangorUnderenhet,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val status: GjennomforingStatusType,
    val prismodell: Prismodell,
) {

    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val tiltakskode: Tiltakskode,
    )

    @Serializable
    data class ArrangorUnderenhet(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val organisasjonsnummer: Organisasjonsnummer,
        val navn: String,
    )
}
