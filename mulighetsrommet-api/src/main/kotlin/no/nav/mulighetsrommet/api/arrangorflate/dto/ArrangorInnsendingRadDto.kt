package no.nav.mulighetsrommet.api.arrangorflate.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateUtbetalingKompaktDto
import no.nav.mulighetsrommet.api.arrangorflate.api.ArrangorflateUtbetalingStatus
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingGruppetiltakKompakt
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

@Serializable
data class ArrangorInnsendingRadDto(
    @Serializable(with = UUIDSerializer::class)
    val gjennomforingId: UUID,
    val arrangorNavn: String,
    val organisasjonsnummer: Organisasjonsnummer,
    val tiltakstypeNavn: String,
    val tiltakNavn: String,
    val lopenummer: Tiltaksnummer,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val belop: ValutaBelop?,
    val type: String?,
    val status: ArrangorflateUtbetalingStatus?,
)

fun GjennomforingGruppetiltakKompakt.toRadDto(): ArrangorInnsendingRadDto = ArrangorInnsendingRadDto(
    gjennomforingId = this.id,
    arrangorNavn = this.arrangor.navn,
    organisasjonsnummer = this.arrangor.organisasjonsnummer,
    tiltakstypeNavn = this.tiltakstype.navn,
    tiltakNavn = this.navn,
    lopenummer = this.lopenummer,
    startDato = this.startDato,
    sluttDato = this.sluttDato,
    belop = null,
    type = null,
    status = null,
)

fun ArrangorflateUtbetalingKompaktDto.toRadDto(): ArrangorInnsendingRadDto = ArrangorInnsendingRadDto(
    gjennomforingId = this.gjennomforing.id,
    arrangorNavn = this.arrangor.navn,
    organisasjonsnummer = this.arrangor.organisasjonsnummer,
    tiltakstypeNavn = this.tiltakstype.navn,
    tiltakNavn = this.gjennomforing.navn,
    lopenummer = this.gjennomforing.lopenummer,
    startDato = this.periode.start,
    sluttDato = this.periode.slutt,
    belop = this.pris,
    type = this.type.displayName,
    status = this.status,
)
