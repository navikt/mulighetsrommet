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
    val prisbetingelser: String?,
    val administratorer: List<Administrator>,
    val antallPlasser: Int?,
    val opphav: ArenaMigrering.Opphav,
    val kontorstruktur: List<Kontorstruktur>,
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
    val personopplysninger: List<Personopplysning>,
    val personvernBekreftet: Boolean,
    val amoKategorisering: AmoKategorisering?,
    val opsjonsmodellData: OpsjonsmodellData?,
    val opsjonerRegistrert: List<OpsjonLoggRegistrert>?,
    val utdanningslop: UtdanningslopDto?,
    val prismodell: Prismodell?,
) {

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
        val status: OpsjonLoggRequest.OpsjonsLoggStatus,
    )

    fun toDbo(): AvtaleDbo {
        return AvtaleDbo(
            id = id,
            navn = navn,
            tiltakstypeId = tiltakstype.id,
            avtalenummer = avtalenummer,
            sakarkivNummer = sakarkivNummer,
            arrangor = arrangor?.id?.let {
                AvtaleDbo.Arrangor(
                    hovedenhet = it,
                    underenheter = arrangor.underenheter.map { it.id },
                    kontaktpersoner = arrangor.kontaktpersoner.map { it.id },
                )
            },
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
    }

    fun toArenaAvtaleDbo(): ArenaAvtaleDbo? {
        return arrangor?.organisasjonsnummer?.value?.let {
            ArenaAvtaleDbo(
                id = id,
                navn = navn,
                tiltakstypeId = tiltakstype.id,
                avtalenummer = avtalenummer,
                arrangorOrganisasjonsnummer = it,
                startDato = startDato,
                sluttDato = sluttDato,
                arenaAnsvarligEnhet = arenaAnsvarligEnhet?.enhetsnummer,
                avtaletype = avtaletype,
                avslutningsstatus = when (status) {
                    is AvtaleStatusDto.Aktiv -> Avslutningsstatus.IKKE_AVSLUTTET
                    is AvtaleStatusDto.Avbrutt -> Avslutningsstatus.AVBRUTT
                    is AvtaleStatusDto.Avsluttet -> Avslutningsstatus.AVSLUTTET
                    is AvtaleStatusDto.Utkast -> Avslutningsstatus.IKKE_AVSLUTTET
                },
                prisbetingelser = prisbetingelser,
            )
        }
    }
}
