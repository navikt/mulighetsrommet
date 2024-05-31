package no.nav.mulighetsrommet.api.domain.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import no.nav.mulighetsrommet.api.domain.dbo.ArenaNavEnhet
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.ArenaAvtaleDbo
import no.nav.mulighetsrommet.domain.dbo.Avslutningsstatus
import no.nav.mulighetsrommet.domain.dto.*
import no.nav.mulighetsrommet.domain.serializers.AvtaleStatusSerializer
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
    val websaknummer: Websaknummer?,
    val lopenummer: Lopenummer?,
    val arrangor: ArrangorHovedenhet,
    @Serializable(with = LocalDateSerializer::class)
    val startDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val sluttDato: LocalDate?,
    val arenaAnsvarligEnhet: ArenaNavEnhet?,
    val avtaletype: Avtaletype,
    @Serializable(with = AvtaleStatusSerializer::class)
    val status: AvtaleStatus,
    val prisbetingelser: String?,
    val administratorer: List<Administrator>,
    val antallPlasser: Int?,
    val opphav: ArenaMigrering.Opphav,
    val kontorstruktur: List<Kontorstruktur>,
    val beskrivelse: String? = null,
    val faneinnhold: Faneinnhold? = null,
    val personopplysninger: List<Personopplysning>,
    val personvernBekreftet: Boolean,
    val nusData: NusData? = null,
) {
    @Serializable
    data class Tiltakstype(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val navn: String,
        val arenaKode: String,
        val tiltakskode: Tiltakskode?,
    )

    @Serializable
    data class ArrangorHovedenhet(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val organisasjonsnummer: String,
        val navn: String,
        val slettet: Boolean,
        val underenheter: List<ArrangorUnderenhet>,
        val kontaktpersoner: List<ArrangorKontaktperson>,
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

    @Serializable
    data class NusData(
        val versjon: String,
        val utdanningsnivaa: String,
        val utdanningskategorier: List<NusDataElement>,
    )

    @Serializable
    data class NusDataElement(
        val code: String,
        val name: String,
    )

    fun toDbo() =
        AvtaleDbo(
            id = id,
            navn = navn,
            tiltakstypeId = tiltakstype.id,
            avtalenummer = avtalenummer,
            websaknummer = websaknummer,
            arrangorId = arrangor.id,
            arrangorUnderenheter = arrangor.underenheter.map { it.id },
            arrangorKontaktpersoner = arrangor.kontaktpersoner.map { it.id },
            startDato = startDato,
            sluttDato = sluttDato,
            navEnheter = this.kontorstruktur.flatMap { it.kontorer.map { kontor -> kontor.enhetsnummer } + it.region.enhetsnummer },
            avtaletype = avtaletype,
            prisbetingelser = prisbetingelser,
            antallPlasser = antallPlasser,
            administratorer = administratorer.map { it.navIdent },
            beskrivelse = null,
            faneinnhold = null,
            personopplysninger = personopplysninger,
            personvernBekreftet = personvernBekreftet,
            nusData = nusData?.let { Json.encodeToJsonElement(it).jsonObject },
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
            avslutningsstatus = when (status) {
                is AvtaleStatus.AKTIV -> Avslutningsstatus.IKKE_AVSLUTTET
                is AvtaleStatus.AVBRUTT -> Avslutningsstatus.AVBRUTT
                is AvtaleStatus.AVSLUTTET -> Avslutningsstatus.AVSLUTTET
            },
            prisbetingelser = prisbetingelser,
        )
}
