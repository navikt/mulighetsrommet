package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.navenhet.db.ArenaNavEnhet
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class AvtaleDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltakstype: Tiltakstype,
    val navn: String,
    val avtalenummer: String?,
    val sakarkivNummer: SakarkivNummer?,
    val arrangor: ArrangorHovedenhet?,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val arenaAnsvarligEnhet: ArenaNavEnhet?,
    val avtaletype: Avtaletype,
    val status: AvtaleStatusDto,
    val administratorer: List<Administrator>,
    val opphav: ArenaMigrering.Opphav,
    val kontorstruktur: List<Kontorstruktur>,
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
    val personopplysninger: List<Personopplysning>,
    val personvernBekreftet: Boolean,
    val amoKategorisering: AmoKategorisering?,
    val opsjonsmodell: Opsjonsmodell,
    val opsjonerRegistrert: List<OpsjonLoggRegistrert>?,
    val utdanningslop: UtdanningslopDto?,
    val prismodell: PrismodellDto,
) {
    @Serializable
    sealed class PrismodellDto {
        @Serializable
        @SerialName("ANNEN_AVTALT_PRIS")
        data class AnnenAvtaltPris(
            val prisbetingelser: String?,
        ) : PrismodellDto()

        @Serializable
        @SerialName("FORHANDSGODKJENT_PRIS_PER_MANEDSVERK")
        data object ForhandsgodkjentPrisPerManedsverk : PrismodellDto()

        @Serializable
        @SerialName("AVTALT_PRIS_PER_MANEDSVERK")
        data class AvtaltPrisPerManedsverk(
            val prisbetingelser: String?,
            val satser: List<AvtaltSatsDto>,
        ) : PrismodellDto()

        @Serializable
        @SerialName("AVTALT_PRIS_PER_UKESVERK")
        data class AvtaltPrisPerUkesverk(
            val prisbetingelser: String?,
            val satser: List<AvtaltSatsDto>,
        ) : PrismodellDto()

        @Serializable
        @SerialName("AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER")
        data class AvtaltPrisPerTimeOppfolgingPerDeltaker(
            val prisbetingelser: String?,
            val satser: List<AvtaltSatsDto>,
        ) : PrismodellDto()
    }

    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val tiltakskode: Tiltakskode,
    )

    @Serializable
    data class ArrangorHovedenhet(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val organisasjonsnummer: Organisasjonsnummer,
        val navn: String,
        val slettet: Boolean,
        val underenheter: List<ArrangorUnderenhet>,
        val kontaktpersoner: List<ArrangorKontaktperson>,
    )

    @Serializable
    data class ArrangorUnderenhet(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val organisasjonsnummer: Organisasjonsnummer,
        val navn: String,
        val slettet: Boolean,
    )

    @Serializable
    data class ArrangorKontaktperson(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val beskrivelse: String?,
        val telefon: String?,
        val epost: String,
    )

    @Serializable
    data class Administrator(
        val navIdent: NavIdent,
        val navn: String,
    )

    @Serializable
    data class OpsjonLoggRegistrert(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        @Serializable(with = LocalDateSerializer::class)
        val registrertDato: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        val sluttDato: LocalDate?,
        @Serializable(with = LocalDateSerializer::class)
        val forrigeSluttdato: LocalDate?,
        val status: OpsjonLoggStatus,
    )
}
