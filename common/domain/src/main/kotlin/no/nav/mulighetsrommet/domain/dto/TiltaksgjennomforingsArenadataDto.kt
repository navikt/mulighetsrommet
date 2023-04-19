package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class TiltaksgjennomforingsArenadataDto(
    val opprettetAar: Int?,
    val lopenr: Int?,
    val virksomhetsnummer: String?,
    val ansvarligNavEnhetId: String?,
    val status: String
) {
    companion object {
        fun from(tiltaksgjennomforing: TiltaksgjennomforingAdminDto, status: String) = tiltaksgjennomforing.run {
            TiltaksgjennomforingsArenadataDto(
                opprettetAar = tiltaksnummer?.split("#")?.first()?.toInt(),
                lopenr = tiltaksnummer?.split("#")?.get(1)?.toInt(),
                virksomhetsnummer = virksomhetsnummer,
                ansvarligNavEnhetId = arenaAnsvarligEnhet,
                status = status
            )
        }
    }
}
