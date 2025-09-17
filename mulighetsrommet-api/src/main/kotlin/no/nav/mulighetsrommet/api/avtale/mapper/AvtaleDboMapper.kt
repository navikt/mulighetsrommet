package no.nav.mulighetsrommet.api.avtale.mapper

import no.nav.mulighetsrommet.api.avtale.AvtaleDetaljerRequest
import no.nav.mulighetsrommet.api.avtale.AvtalePersonvernRequest
import no.nav.mulighetsrommet.api.avtale.api.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.AvtaleVeilederinfoRequest
import no.nav.mulighetsrommet.api.avtale.db.ArrangorDbo
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.db.DetaljerDbo
import no.nav.mulighetsrommet.api.avtale.db.OpsjonsmodellDbo
import no.nav.mulighetsrommet.api.avtale.db.PersonvernDbo
import no.nav.mulighetsrommet.api.avtale.db.RedaksjoneltInnholdDbo
import no.nav.mulighetsrommet.api.avtale.db.VeilederinformasjonDbo
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.Opsjonsmodell
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import java.util.UUID
import java.time.LocalDate
import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSatsDto
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSatsRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellRequest
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Tiltakskode

object AvtaleDboMapper {
    fun fromAvtale(avtale: Avtale) = AvtaleDbo(
        id = avtale.id,
        status = avtale.status.type,
        avtalenummer = avtale.avtalenummer,
        detaljer = DetaljerDbo(
            navn = avtale.navn,
            sakarkivnummer = avtale.sakarkivNummer?.value,
            arrangor = avtale.arrangor?.id?.let {
                ArrangorDbo(
                    hovedenhet = it,
                    underenheter = avtale.arrangor.underenheter.map { it.id },
                    kontaktpersoner = avtale.arrangor.kontaktpersoner.map { it.id },
                )
            },
            tiltakstypeId = avtale.tiltakstype.id,
            avtaletype = avtale.avtaletype,
            administratorer = avtale.administratorer.map { it.navIdent },
            prismodell = avtale.prismodell.prismodell(),
            prisbetingelser = avtale.prismodell.prisbetingelser(),
            satser = avtale.prismodell.satser(),
            startDato = avtale.startDato,
            sluttDato = avtale.sluttDato,
            amoKategorisering = avtale.amoKategorisering,
            opsjonsmodell = OpsjonsmodellDbo(
                type = avtale.opsjonsmodell.type,
                maksVarighet = avtale.opsjonsmodell.maksVarighet,
                customNavn = avtale.opsjonsmodell.customNavn,
            ),
            utdanningslop = avtale.utdanningslop?.toDbo(),
        ),
        veilederinformasjon = VeilederinformasjonDbo(
            navEnheter = avtale.kontorstruktur.flatMap {
                it.kontorer.map { kontor -> kontor.enhetsnummer } + it.region.enhetsnummer
            }.toSet(),
            redaksjoneltInnhold = null,
        ),
        personvern = PersonvernDbo(
            personopplysninger = avtale.personopplysninger,
            personvernBekreftet = avtale.personvernBekreftet,
        ),

    )

    fun fromAvtaleRequest(
        request: AvtaleRequest,
        startDato: LocalDate,
        prismodellDbo: PrismodellDbo,
        arrangor: ArrangorDbo?,
        status: AvtaleStatusType,
        tiltakstypeId: UUID,
    ): AvtaleDbo = AvtaleDbo(
        id = request.id,
        status = status,
        detaljer = request.detaljer.toDbo(tiltakstypeId, arrangor),
        veilederinformasjon = request.veilederinformasjon.toDbo(),
        personvern = request.personvern.toDbo(),
        avtalenummer = null,
    )

    fun AvtaleDetaljerRequest.toDbo(tiltakstypeId: UUID, arrangor: ArrangorDbo?): DetaljerDbo = DetaljerDbo(
        navn = navn,
        tiltakstypeId = tiltakstypeId,
        sakarkivnummer = sakarkivNummer?.value,
        startDato = startDato,
        sluttDato = sluttDato,
        avtaletype = avtaletype,
        administratorer = administratorer,
        prismodell = PrismodellRequest(
            type = prismodell,
            prisbetingelser = prisbetingelser,
            satser = satser.map {
                AvtaltSatsRequest(
                    pris = it.sats,
                    valuta = "NOK",
                    gjelderFra = it.gjelderFra,
                )
            },
        ),
        arrangor = arrangor,
        opsjonsmodell = opsjonsmodell.toDbo(),
        amoKategorisering = amoKategorisering,
        utdanningslop = utdanningslop,
    )

    fun toAvtaleRequest(dbo: AvtaleDbo, arrangor: AvtaleRequest.Arrangor?, tiltakskode: Tiltakskode) = AvtaleRequest(
        id = dbo.id,
        navn = dbo.detaljer.navn,
        avtalenummer = dbo.avtalenummer,
        sakarkivNummer = dbo.detaljer.sakarkivNummer,
        tiltakskode = tiltakskode,
        arrangor = arrangor,
        startDato = dbo.detaljer.startDato,
        sluttDato = dbo.detaljer.sluttDato,
        avtaletype = dbo.detaljer.avtaletype,
        administratorer = dbo.detaljer.administratorer,
        navEnheter = dbo.veilederinformasjon.navEnheter.toList(),
        beskrivelse = dbo.veilederinformasjon.redaksjoneltInnhold?.beskrivelse,
        faneinnhold = dbo.veilederinformasjon.redaksjoneltInnhold?.faneinnhold,
        personopplysninger = dbo.personvern.personopplysninger,
        personvernBekreftet = dbo.personvern.personvernBekreftet,
        amoKategorisering = dbo.detaljer.amoKategorisering,
        opsjonsmodell = dbo.detaljer.opsjonsmodell,
        utdanningslop = dbo.detaljer.utdanningslop,
        prismodell = PrismodellRequest(
            type = dbo.detaljer.prismodell,
            prisbetingelser = dbo.detaljer.prisbetingelser,
            satser = dbo.detaljer.satser.map {
                AvtaltSatsRequest(
                    pris = it.sats,
                    valuta = "NOK",
                    gjelderFra = it.gjelderFra,
                )
            },
        ),
    )

    fun AvtalePersonvernRequest.toDbo(): PersonvernDbo = PersonvernDbo(
        personvernBekreftet = personvernBekreftet,
        personopplysninger = personopplysninger,
    )

    fun AvtaleVeilederinfoRequest.toDbo(): VeilederinformasjonDbo = VeilederinformasjonDbo(
        redaksjoneltInnhold = RedaksjoneltInnholdDbo(
            beskrivelse = beskrivelse,
            faneinnhold = faneinnhold,
        ),
        navEnheter = navEnheter,
    )

    fun Opsjonsmodell.toDbo(): OpsjonsmodellDbo = OpsjonsmodellDbo(
        type = type,
        maksVarighet = maksVarighet,
        customNavn = customNavn,

        )
}

fun Avtale.PrismodellDto.prismodell(): Prismodell = when (this) {
    is Avtale.PrismodellDto.AnnenAvtaltPris -> Prismodell.ANNEN_AVTALT_PRIS
    is Avtale.PrismodellDto.AvtaltPrisPerManedsverk -> Prismodell.AVTALT_PRIS_PER_MANEDSVERK
    is Avtale.PrismodellDto.AvtaltPrisPerUkesverk -> Prismodell.AVTALT_PRIS_PER_UKESVERK
    is Avtale.PrismodellDto.ForhandsgodkjentPrisPerManedsverk -> Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK
    is Avtale.PrismodellDto.AvtaltPrisPerTimeOppfolgingPerDeltaker -> Prismodell.AVTALT_PRIS_PER_TIME_OPPFOLGING_PER_DELTAKER
}

fun Avtale.PrismodellDto.prisbetingelser(): String? = when (this) {
    is Avtale.PrismodellDto.AnnenAvtaltPris -> prisbetingelser
    is Avtale.PrismodellDto.AvtaltPrisPerManedsverk -> prisbetingelser
    is Avtale.PrismodellDto.AvtaltPrisPerUkesverk -> prisbetingelser
    is Avtale.PrismodellDto.AvtaltPrisPerTimeOppfolgingPerDeltaker -> prisbetingelser
    Avtale.PrismodellDto.ForhandsgodkjentPrisPerManedsverk -> null
}

fun Avtale.PrismodellDto.satser(): List<AvtaltSats> = when (this) {
    is Avtale.PrismodellDto.AnnenAvtaltPris,
    is Avtale.PrismodellDto.ForhandsgodkjentPrisPerManedsverk,
    -> emptyList()

    is Avtale.PrismodellDto.AvtaltPrisPerManedsverk -> toAvtalteSatser(satser)
    is Avtale.PrismodellDto.AvtaltPrisPerUkesverk -> toAvtalteSatser(satser)
    is Avtale.PrismodellDto.AvtaltPrisPerTimeOppfolgingPerDeltaker -> toAvtalteSatser(satser)
}

private fun toAvtalteSatser(satser: List<AvtaltSatsDto>): List<AvtaltSats> = satser.map {
    AvtaltSats(
        gjelderFra = it.gjelderFra,
        sats = it.pris,
    )
}
