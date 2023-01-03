package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class TiltaksgjennomforingsArenadataDto(
    val opprettetAar: Int,
    val lopenr: Int,
    val virksomhetsnummer: String?,
    val ansvarligNavEnhetId: String
) {
    companion object {
        fun from(tiltaksgjennomforing: TiltaksgjennomforingAdminDto) = tiltaksgjennomforing.run {
            TiltaksgjennomforingsArenadataDto(
                opprettetAar = tiltaksnummer.split("#").first().toInt(),
                lopenr = tiltaksnummer.split("#")[1].toInt(),
                virksomhetsnummer = virksomhetsnummer,
                ansvarligNavEnhetId = enhet
            )
        }
    }
}
