package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.domain.dbo.ArenaNavEnhet
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.ArenaAvtaleDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.Faneinnhold
import no.nav.mulighetsrommet.domain.dto.NavIdent
import no.nav.mulighetsrommet.domain.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
import java.time.LocalDate
import java.util.*

@Serializable
data class AvtaleAdminDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val tiltakstype: Tiltakstype,
    val navn: String,
    val avtalenummer: String?,
    val arrangor: ArrangorHovedenhet,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val arenaAnsvarligEnhet: ArenaNavEnhet?,
    val avtaletype: Avtaletype,
    val avtalestatus: Avtalestatus,
    val prisbetingelser: String?,
    val administratorer: List<Administrator>,
    val url: String?,
    val antallPlasser: Int?,
    val opphav: ArenaMigrering.Opphav,
    val kontorstruktur: List<Kontorstruktur>,
    val beskrivelse: String? = null,
    val faneinnhold: Faneinnhold? = null,
) {
    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val arenaKode: String,
    )

    @Serializable
    data class ArrangorHovedenhet(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val organisasjonsnummer: String,
        val navn: String,
        val slettet: Boolean,
        val underenheter: List<ArrangorUnderenhet>,
        val kontaktperson: ArrangorKontaktperson?,
    )

    @Serializable
    data class ArrangorUnderenhet(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val organisasjonsnummer: String,
        val navn: String,
        val slettet: Boolean,
        // TODO: denne er hardkodet til emptyList() enn så lenge slik at modell matcher [TiltaksgjennomforingAdminDto.ArrangorUnderenhet] samt modell i openapi.yaml
        //  satser på å få samlet modellene i neste omgang.
        val kontaktpersoner: List<ArrangorKontaktperson> = emptyList(),
    )

    @Serializable
    data class Administrator(
        val navIdent: NavIdent,
        val navn: String,
    )

    fun toDbo() =
        AvtaleDbo(
            id = id,
            navn = navn,
            tiltakstypeId = tiltakstype.id,
            avtalenummer = avtalenummer,
            arrangorId = arrangor.id,
            arrangorUnderenheter = arrangor.underenheter.map { it.id },
            arrangorKontaktpersonId = arrangor.kontaktperson?.id,
            startDato = startDato,
            sluttDato = sluttDato,
            navEnheter = this.kontorstruktur.flatMap { it.kontorer.map { kontor -> kontor.enhetsnummer } + it.region.enhetsnummer },
            avtaletype = avtaletype,
            prisbetingelser = prisbetingelser,
            antallPlasser = antallPlasser,
            url = url,
            administratorer = administratorer.map { it.navIdent },
            beskrivelse = null,
            faneinnhold = null,
        )

    fun toArenaAvtaleDbo() =
        ArenaAvtaleDbo(
            id = id,
            navn = navn,
            tiltakstypeId = tiltakstype.id,
            avtalenummer = avtalenummer,
            arrangorOrganisasjonsnummer = arrangor.organisasjonsnummer,
            startDato = startDato,
            sluttDato = sluttDato,
            arenaAnsvarligEnhet = arenaAnsvarligEnhet?.enhetsnummer,
            avtaletype = avtaletype,
            avslutningsstatus = when (avtalestatus) {
                Avtalestatus.Aktiv -> Avslutningsstatus.IKKE_AVSLUTTET
                Avtalestatus.Avbrutt -> Avslutningsstatus.AVBRUTT
                Avtalestatus.Avsluttet -> Avslutningsstatus.AVSLUTTET
            },
            prisbetingelser = prisbetingelser,
        )
}
