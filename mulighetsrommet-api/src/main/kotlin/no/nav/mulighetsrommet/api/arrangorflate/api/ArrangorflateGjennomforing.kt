package no.nav.mulighetsrommet.api.arrangorflate.api

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingKompakt
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class ArrangorflateGjennomforing(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val tiltakstype: ArrangorflateTiltakstype,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
) {
    companion object {
        fun fromGjennomforing(gjennomforing: Gjennomforing) = ArrangorflateGjennomforing(
            id = gjennomforing.id,
            navn = gjennomforing.navn,
            tiltakstype = ArrangorflateTiltakstype(
                navn = gjennomforing.tiltakstype.navn,
                tiltakskode = gjennomforing.tiltakstype.tiltakskode,
            ),
            startDato = gjennomforing.startDato,
            sluttDato = gjennomforing.sluttDato,
        )

        fun fromGjennomforingKompakt(gjennomforing: GjennomforingKompakt) = ArrangorflateGjennomforing(
            id = gjennomforing.id,
            navn = gjennomforing.navn,
            tiltakstype = ArrangorflateTiltakstype(
                navn = gjennomforing.tiltakstype.navn,
                tiltakskode = gjennomforing.tiltakstype.tiltakskode,
            ),
            startDato = gjennomforing.startDato,
            sluttDato = gjennomforing.sluttDato,
        )
    }
}

@Serializable
data class ArrangorflateGjennomforingInfo(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
    val lopenummer: Tiltaksnummer,
)
