package no.nav.mulighetsrommet.api.avtale.model

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.avtale.OpsjonLoggRequest
import no.nav.mulighetsrommet.api.avtale.OpsjonsmodellData
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.navenhet.db.ArenaNavEnhet
import no.nav.mulighetsrommet.arena.ArenaAvtaleDbo
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.arena.Avslutningsstatus
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.AvtaleStatusSerializer
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import no.nav.mulighetsrommet.serializers.LocalDateTimeSerializer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

// @Todo: Look into sealed classes
@Serializable
sealed class AvtaleDto {
    abstract val id: UUID
    abstract val tiltakstype: Tiltakstype
    abstract val navn: String
    abstract val avtalenummer: String?
    abstract val websaknummer: Websaknummer?
    abstract val startDato: LocalDate
    abstract val sluttDato: LocalDate?
    abstract val arenaAnsvarligEnhet: ArenaNavEnhet?
    abstract val avtaletype: Avtaletype
    abstract val status: AvtaleStatus
    abstract val prisbetingelser: String?
    abstract val administratorer: List<Administrator>
    abstract val antallPlasser: Int?
    abstract val opphav: ArenaMigrering.Opphav
    abstract val kontorstruktur: List<Kontorstruktur>
    abstract val beskrivelse: String?
    abstract val faneinnhold: Faneinnhold?
    abstract val personopplysninger: List<Personopplysning>
    abstract val personvernBekreftet: Boolean
    abstract val amoKategorisering: AmoKategorisering?
    abstract val opsjonsmodellData: OpsjonsmodellData?
    abstract val opsjonerRegistrert: List<OpsjonLoggRegistrert>?
    abstract val utdanningslop: UtdanningslopDto?
    abstract val prismodell: Prismodell?

    @Serializable
    data class WithArrangor(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val tiltakstype: Tiltakstype,
        override val navn: String,
        override val avtalenummer: String?,
        override val websaknummer: Websaknummer?,
        val arrangor: ArrangorHovedenhet,
        @Serializable(with = LocalDateSerializer::class)
        override val startDato: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        override val sluttDato: LocalDate?,
        override val arenaAnsvarligEnhet: ArenaNavEnhet?,
        override val avtaletype: Avtaletype,
        @Serializable(with = AvtaleStatusSerializer::class)
        override val status: AvtaleStatus,
        override val prisbetingelser: String?,
        override val administratorer: List<Administrator>,
        override val antallPlasser: Int?,
        override val opphav: ArenaMigrering.Opphav,
        override val kontorstruktur: List<Kontorstruktur>,
        override val beskrivelse: String?,
        override val faneinnhold: Faneinnhold?,
        override val personopplysninger: List<Personopplysning>,
        override val personvernBekreftet: Boolean,
        override val amoKategorisering: AmoKategorisering?,
        override val opsjonsmodellData: OpsjonsmodellData?,
        override val opsjonerRegistrert: List<OpsjonLoggRegistrert>?,
        override val utdanningslop: UtdanningslopDto?,
        override val prismodell: Prismodell?,
    ) : AvtaleDto()

    @Serializable
    data class WithoutArrangor(
        @Serializable(with = UUIDSerializer::class)
        override val id: UUID,
        override val tiltakstype: Tiltakstype,
        override val navn: String,
        override val avtalenummer: String?,
        override val websaknummer: Websaknummer?,
        @Serializable(with = LocalDateSerializer::class)
        override val startDato: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        override val sluttDato: LocalDate?,
        override val arenaAnsvarligEnhet: ArenaNavEnhet?,
        override val avtaletype: Avtaletype,
        @Serializable(with = AvtaleStatusSerializer::class)
        override val status: AvtaleStatus,
        override val prisbetingelser: String?,
        override val administratorer: List<Administrator>,
        override val antallPlasser: Int?,
        override val opphav: ArenaMigrering.Opphav,
        override val kontorstruktur: List<Kontorstruktur>,
        override val beskrivelse: String?,
        override val faneinnhold: Faneinnhold?,
        override val personopplysninger: List<Personopplysning>,
        override val personvernBekreftet: Boolean,
        override val amoKategorisering: AmoKategorisering?,
        override val opsjonsmodellData: OpsjonsmodellData?,
        override val opsjonerRegistrert: List<OpsjonLoggRegistrert>?,
        override val utdanningslop: UtdanningslopDto?,
        override val prismodell: Prismodell?,
    ) : AvtaleDto()

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
        // TODO: denne er hardkodet til emptyList() enn så lenge slik at modell matcher [GjennomforingDto.ArrangorUnderenhet] samt modell i openapi.yaml
        //  satser på å få samlet modellene i neste omgang.
        val kontaktpersoner: List<ArrangorKontaktperson> = emptyList(),
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
        @Serializable(with = LocalDateTimeSerializer::class)
        val aktivertDato: LocalDateTime,
        @Serializable(with = LocalDateTimeSerializer::class)
        val sluttDato: LocalDateTime?,
        @Serializable(with = LocalDateTimeSerializer::class)
        val forrigeSluttdato: LocalDateTime?,
        val status: OpsjonLoggRequest.OpsjonsLoggStatus,
    )
}

fun AvtaleDto.toDbo() = when (this) {
    is AvtaleDto.WithArrangor -> AvtaleDbo(
        id = id,
        navn = navn,
        tiltakstypeId = tiltakstype.id,
        avtalenummer = avtalenummer,
        websaknummer = websaknummer,
        arrangor = AvtaleDbo.Arrangor(
            hovedenhet = arrangor.id,
            underenheter = arrangor.underenheter.map { it.id },
            kontaktpersoner = arrangor.kontaktpersoner.map { it.id },
        ),
        startDato = startDato,
        sluttDato = sluttDato,
        navEnheter = kontorstruktur.flatMap { it.kontorer.map { kontor -> kontor.enhetsnummer } + it.region.enhetsnummer },
        avtaletype = avtaletype,
        prisbetingelser = prisbetingelser,
        antallPlasser = antallPlasser,
        administratorer = administratorer.map { it.navIdent },
        beskrivelse = null,
        faneinnhold = null,
        personopplysninger = personopplysninger,
        personvernBekreftet = personvernBekreftet,
        amoKategorisering = amoKategorisering,
        opsjonMaksVarighet = opsjonsmodellData?.opsjonMaksVarighet,
        opsjonsmodell = opsjonsmodellData?.opsjonsmodell,
        customOpsjonsmodellNavn = opsjonsmodellData?.customOpsjonsmodellNavn,
        utdanningslop = utdanningslop?.toDbo(),
        prismodell = prismodell,
    )
    is AvtaleDto.WithoutArrangor -> throw IllegalStateException("Cannot convert AvtaleDto without arrangor to AvtaleDbo")
}

fun AvtaleDto.toArenaAvtaleDbo() = when (this) {
    is AvtaleDto.WithArrangor -> ArenaAvtaleDbo(
        id = id,
        navn = navn,
        tiltakstypeId = tiltakstype.id,
        avtalenummer = avtalenummer,
        arrangorOrganisasjonsnummer = arrangor.organisasjonsnummer.value,
        startDato = startDato,
        sluttDato = sluttDato,
        arenaAnsvarligEnhet = arenaAnsvarligEnhet?.enhetsnummer,
        avtaletype = avtaletype,
        avslutningsstatus = when (status) {
            is AvtaleStatus.AKTIV -> Avslutningsstatus.IKKE_AVSLUTTET
            is AvtaleStatus.AVBRUTT -> Avslutningsstatus.AVBRUTT
            is AvtaleStatus.UTKAST -> Avslutningsstatus.UTKAST
            is AvtaleStatus.AVSLUTTET -> Avslutningsstatus.AVSLUTTET
        },
        prisbetingelser = prisbetingelser,
    )
    is AvtaleDto.WithoutArrangor -> throw IllegalStateException("Cannot convert AvtaleDto without arrangor to ArenaAvtaleDbo")
}
