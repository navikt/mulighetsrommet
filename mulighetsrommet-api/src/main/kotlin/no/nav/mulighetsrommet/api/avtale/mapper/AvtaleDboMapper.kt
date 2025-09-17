package no.nav.mulighetsrommet.api.avtale.mapper

import no.nav.mulighetsrommet.api.avtale.AvtaleDetaljerRequest
import no.nav.mulighetsrommet.api.avtale.AvtalePersonvernRequest
import no.nav.mulighetsrommet.api.avtale.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.AvtaleVeilederinfoRequest
import no.nav.mulighetsrommet.api.avtale.db.ArrangorDbo
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.db.DetaljerDbo
import no.nav.mulighetsrommet.api.avtale.db.OpsjonsmodellDbo
import no.nav.mulighetsrommet.api.avtale.db.PersonvernDbo
import no.nav.mulighetsrommet.api.avtale.db.RedaksjoneltInnholdDbo
import no.nav.mulighetsrommet.api.avtale.db.VeilederinformasjonDbo
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.Opsjonsmodell
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.model.AvtaleStatus
import no.nav.mulighetsrommet.model.Periode
import java.util.UUID

object AvtaleDboMapper {
    fun fromAvtaleDto(dto: AvtaleDto) = AvtaleDbo(
        id = dto.id,
        status = dto.status.type,
        avtalenummer = dto.avtalenummer,
        detaljer = DetaljerDbo(
            navn = dto.navn,
            sakarkivnummer = dto.sakarkivNummer?.value,
            arrangor = dto.arrangor?.id?.let {
                ArrangorDbo(
                    hovedenhet = it,
                    underenheter = dto.arrangor.underenheter.map { it.id },
                    kontaktpersoner = dto.arrangor.kontaktpersoner.map { it.id },
                )
            },
            tiltakstypeId = dto.tiltakstype.id,
            avtaletype = dto.avtaletype,
            administratorer = dto.administratorer.map { it.navIdent },
            prismodell = dto.prismodell.prismodell(),
            prisbetingelser = when (dto.prismodell) {
                is AvtaleDto.PrismodellDto.AnnenAvtaltPris -> dto.prismodell.prisbetingelser
                is AvtaleDto.PrismodellDto.AvtaltPrisPerManedsverk -> dto.prismodell.prisbetingelser
                is AvtaleDto.PrismodellDto.AvtaltPrisPerUkesverk -> dto.prismodell.prisbetingelser
                AvtaleDto.PrismodellDto.ForhandsgodkjentPrisPerManedsverk -> null
            },
            satser = dto.prismodell.satser(),
            startDato = dto.startDato,
            sluttDato = dto.sluttDato,
            amoKategorisering = dto.amoKategorisering,
            opsjonsmodell = OpsjonsmodellDbo(
                type = dto.opsjonsmodell.type,
                maksVarighet = dto.opsjonsmodell.maksVarighet,
                customNavn = dto.opsjonsmodell.customNavn,
            ),
            utdanningslop = dto.utdanningslop?.toDbo(),
        ),
        veilederinformasjon = VeilederinformasjonDbo(
            navEnheter = dto.kontorstruktur.flatMap {
                it.kontorer.map { kontor -> kontor.enhetsnummer } + it.region.enhetsnummer
            },
            redaksjoneltInnhold = null,
        ),
        personvern = PersonvernDbo(
            personopplysninger = dto.personopplysninger,
            personvernBekreftet = dto.personvernBekreftet,
        ),

    )

    fun fromAvtaleRequest(
        request: AvtaleRequest,
        arrangor: ArrangorDbo?,
        status: AvtaleStatus,
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
        prismodell = prismodell.type,
        prisbetingelser = prismodell.prisbetingelser,
        satser = prismodell.satser.map {
            AvtaltSats(
                periode = Periode.fromInclusiveDates(it.periodeStart, it.periodeSlutt),
                sats = it.pris,
            )
        },
        arrangor = arrangor,
        opsjonsmodell = opsjonsmodell.toDbo(),
        amoKategorisering = amoKategorisering,
        utdanningslop = utdanningslop,
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

fun AvtaleDto.PrismodellDto.prismodell(): Prismodell = when (this) {
    is AvtaleDto.PrismodellDto.AnnenAvtaltPris -> Prismodell.ANNEN_AVTALT_PRIS
    is AvtaleDto.PrismodellDto.AvtaltPrisPerManedsverk -> Prismodell.AVTALT_PRIS_PER_MANEDSVERK
    is AvtaleDto.PrismodellDto.AvtaltPrisPerUkesverk -> Prismodell.AVTALT_PRIS_PER_UKESVERK
    is AvtaleDto.PrismodellDto.ForhandsgodkjentPrisPerManedsverk -> Prismodell.FORHANDSGODKJENT_PRIS_PER_MANEDSVERK
}

fun AvtaleDto.PrismodellDto.satser(): List<AvtaltSats> = when (this) {
    is AvtaleDto.PrismodellDto.AnnenAvtaltPris,
    is AvtaleDto.PrismodellDto.ForhandsgodkjentPrisPerManedsverk,
    -> emptyList()

    is AvtaleDto.PrismodellDto.AvtaltPrisPerManedsverk ->
        this.satser.map {
            AvtaltSats(
                periode = Periode.fromInclusiveDates(it.periodeStart, it.periodeSlutt),
                sats = it.pris,
            )
        }

    is AvtaleDto.PrismodellDto.AvtaltPrisPerUkesverk ->
        this.satser.map {
            AvtaltSats(
                periode = Periode.fromInclusiveDates(it.periodeStart, it.periodeSlutt),
                sats = it.pris,
            )
        }
}
