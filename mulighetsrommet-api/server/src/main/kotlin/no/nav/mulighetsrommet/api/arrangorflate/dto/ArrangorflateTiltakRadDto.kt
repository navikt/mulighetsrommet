package no.nav.mulighetsrommet.api.arrangorflate.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangorflate.model.ArrangorflateTiltak
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.UUID

@Serializable
data class ArrangorflateTiltakRadDto(
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
    val sluttDato: LocalDate?, // Eksklusive
)

fun ArrangorflateTiltak.toRadDto(): ArrangorflateTiltakRadDto = ArrangorflateTiltakRadDto(
    gjennomforingId = this.id,
    arrangorNavn = this.arrangor.navn,
    organisasjonsnummer = this.arrangor.organisasjonsnummer,
    tiltakstypeNavn = this.tiltakstype.navn,
    tiltakNavn = this.navn,
    lopenummer = this.lopenummer,
    startDato = this.startDato,
    sluttDato = this.sluttDato?.plusDays(1), // Eksklusive
)
