package no.nav.mulighetsrommet.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingTilgjengelighetsstatus
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class TiltaksgjennomforingAdminDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltakstype: Tiltakstype,
    val navn: String,
    val tiltaksnummer: String?,
    val arrangorOrganisasjonsnummer: String,
    val arrangorNavn: String?,
    val arrangorKontaktperson: VirksomhetKontaktperson?,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val arenaAnsvarligEnhet: String?,
    val status: Tiltaksgjennomforingsstatus,
    val tilgjengelighet: TiltaksgjennomforingTilgjengelighetsstatus,
    val estimertVentetid: String?,
    val antallPlasser: Int?,
    @Serializable(with = UUIDSerializer::class)
    val avtaleId: UUID?,
    val ansvarlig: Ansvarlig?,
    val navEnheter: List<NavEnhet>,
    val navRegion: String?,
    val sanityId: String?,
    val oppstart: TiltaksgjennomforingOppstartstype,
    val opphav: ArenaMigrering.Opphav,
    @Serializable(with = LocalDateSerializer::class)
    val stengtFra: LocalDate?,
    @Serializable(with = LocalDateSerializer::class)
    val stengtTil: LocalDate?,
    val kontaktpersoner: List<TiltaksgjennomforingKontaktperson>,
    val lokasjonArrangor: String?,
) {
    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val arenaKode: String,
    )

    @Serializable
    data class Ansvarlig(
        val navident: String? = null,
        val navn: String? = null,
    )
}
