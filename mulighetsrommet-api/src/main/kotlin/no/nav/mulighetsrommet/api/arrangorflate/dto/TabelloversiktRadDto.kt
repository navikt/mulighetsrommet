package no.nav.mulighetsrommet.api.arrangorflate.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingGruppetiltakKompakt
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID
import no.nav.mulighetsrommet.model.Organisasjonsnummer

@Serializable
data class TabelloversiktRadDto(
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    val arrangorNavn: String,
    val organisasjonsnummer: Organisasjonsnummer,
    val tiltakstypeNavn: String,
    val tiltakNavn: String,
    val lopenummer: Tiltaksnummer,
    val tiltakskode: Tiltakskode,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val belop: Int?,
    val type: String?,
    val status: String?,
)

fun GjennomforingGruppetiltakKompakt.toDto(): TabelloversiktRadDto = TabelloversiktRadDto(
    gjennomforingId = this.id,
    arrangorNavn = this.arrangor.navn,
    organisasjonsnummer = this.arrangor.organisasjonsnummer,
    tiltakstypeNavn = this.tiltakstype.navn,
    tiltakNavn = this.navn,
    lopenummer = this.lopenummer,
    tiltakskode = this.tiltakstype.tiltakskode,
    startDato = this.startDato,
    sluttDato = this.sluttDato,
    belop = null,
    type = null,
    status = null,
)
