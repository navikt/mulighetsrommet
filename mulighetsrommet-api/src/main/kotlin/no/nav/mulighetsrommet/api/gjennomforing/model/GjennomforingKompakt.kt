package no.nav.mulighetsrommet.api.gjennomforing.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.avtale.model.Kontorstruktur
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

sealed class GjennomforingKompakt {
    abstract val id: UUID
    abstract val lopenummer: Tiltaksnummer
    abstract val tiltakstype: Tiltakstype
    abstract val arrangor: ArrangorUnderenhet
    abstract val startDato: LocalDate
    abstract val sluttDato: LocalDate?
    abstract val status: GjennomforingStatus

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

data class AvtaleGjennomforingKompakt(
    override val id: UUID,
    override val lopenummer: Tiltaksnummer,
    override val tiltakstype: Tiltakstype,
    override val arrangor: ArrangorUnderenhet,
    override val startDato: LocalDate,
    override val sluttDato: LocalDate?,
    override val status: GjennomforingStatus,
    val navn: String,
    val kontorstruktur: List<Kontorstruktur>,
    val publisert: Boolean,
) : GjennomforingKompakt()

data class EnkeltplassGjennomforingKompakt(
    override val id: UUID,
    override val lopenummer: Tiltaksnummer,
    override val tiltakstype: Tiltakstype,
    override val arrangor: ArrangorUnderenhet,
    override val startDato: LocalDate,
    override val sluttDato: LocalDate?,
    override val status: GjennomforingStatus,
) : GjennomforingKompakt()
