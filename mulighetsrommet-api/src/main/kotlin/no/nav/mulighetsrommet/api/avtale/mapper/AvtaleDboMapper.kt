package no.nav.mulighetsrommet.api.avtale.mapper

import PersonvernDbo
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.avtale.api.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.DetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.PersonvernRequest
import no.nav.mulighetsrommet.api.avtale.api.VeilederinfoRequest
import no.nav.mulighetsrommet.api.avtale.db.ArrangorDbo
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.db.DetaljerDbo
import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.db.RedaksjoneltInnholdDbo
import no.nav.mulighetsrommet.api.avtale.db.VeilederinformasjonDbo
import no.nav.mulighetsrommet.api.avtale.model.*
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.*

object AvtaleDboMapper {
    fun fromAvtale(avtale: Avtale) = AvtaleDbo(
        id = avtale.id,
        detaljerDbo = DetaljerDbo(
            navn = avtale.navn,
            avtaletype = avtale.avtaletype,
            tiltakstypeId = avtale.tiltakstype.id,
            sakarkivNummer = avtale.sakarkivNummer,
            arrangor = avtale.arrangor?.id?.let {
                ArrangorDbo(
                    hovedenhet = it,
                    underenheter = avtale.arrangor.underenheter.map { it.id },
                    kontaktpersoner = avtale.arrangor.kontaktpersoner.map { it.id },
                )
            },
            startDato = avtale.startDato,
            sluttDato = avtale.sluttDato,
            status = avtale.status.type,
            amoKategorisering = avtale.amoKategorisering,
            opsjonsmodell = avtale.opsjonsmodell,
            utdanningslop = avtale.utdanningslop?.toDbo(),
            administratorer = avtale.administratorer.map { it.navIdent },
        ),
        personvernDbo = PersonvernDbo(
            personopplysninger = avtale.personopplysninger,
            personvernBekreftet = avtale.personvernBekreftet,
        ),
        veilederinformasjonDbo = VeilederinformasjonDbo(
            RedaksjoneltInnholdDbo(
                beskrivelse = avtale.beskrivelse,
                faneinnhold = avtale.faneinnhold,
            ),
            navEnheter = avtale.kontorstruktur.flatMap {
                it.kontorer.map { kontor -> kontor.enhetsnummer } + it.region.enhetsnummer
            }.toSet(),
        ),
        prismodellDbo =
        PrismodellDbo(
            prismodellType = avtale.prismodell.type,
            prisbetingelser = avtale.prismodell.prisbetingelser(),
            satser = avtale.prismodell.satser(),
        ),
    )

    fun fromValidatedAvtaleRequest(
        avtaleId: UUID,
        detaljerDbo: DetaljerDbo,
        prismodellDbo: PrismodellDbo,
        personvernDbo: PersonvernDbo,
        veilederinformasjonDbo: VeilederinformasjonDbo,
    ): AvtaleDbo = AvtaleDbo(
        id = avtaleId,
        detaljerDbo = detaljerDbo,
        prismodellDbo = prismodellDbo,
        personvernDbo = personvernDbo,
        veilederinformasjonDbo = veilederinformasjonDbo,
    )

    fun toAvtaleRequest(dbo: AvtaleDbo, arrangor: DetaljerRequest.Arrangor?, tiltakskode: Tiltakskode) = AvtaleRequest(
        id = dbo.id,
        detaljer = DetaljerRequest(
            navn = dbo.detaljerDbo.navn,
            sakarkivNummer = dbo.detaljerDbo.sakarkivNummer,
            tiltakskode = tiltakskode,
            arrangor = arrangor,
            startDato = dbo.detaljerDbo.startDato,
            sluttDato = dbo.detaljerDbo.sluttDato,
            avtaletype = dbo.detaljerDbo.avtaletype,
            administratorer = dbo.detaljerDbo.administratorer,
            amoKategorisering = dbo.detaljerDbo.amoKategorisering,
            opsjonsmodell = dbo.detaljerDbo.opsjonsmodell,
            utdanningslop = dbo.detaljerDbo.utdanningslop,
        ),

        veilederinformasjon = VeilederinfoRequest(
            navEnheter = dbo.veilederinformasjonDbo.navEnheter.toList(),
            beskrivelse = dbo.veilederinformasjonDbo.redaksjoneltInnhold?.beskrivelse,
            faneinnhold = dbo.veilederinformasjonDbo.redaksjoneltInnhold?.faneinnhold,
        ),
        personvern = PersonvernRequest(
            personopplysninger = dbo.personvernDbo.personopplysninger,
            personvernBekreftet = dbo.personvernDbo.personvernBekreftet,
        ),
        prismodell = PrismodellRequest(
            type = dbo.prismodellDbo.prismodellType,
            prisbetingelser = dbo.prismodellDbo.prisbetingelser,
            satser = dbo.prismodellDbo.satser.map {
                AvtaltSatsRequest(
                    pris = it.sats,
                    valuta = "NOK",
                    gjelderFra = it.gjelderFra,
                )
            },
        ),
    )
}

fun Prismodell.prisbetingelser(): String? = when (this) {
    is Prismodell.AnnenAvtaltPris -> prisbetingelser
    is Prismodell.AvtaltPrisPerManedsverk -> prisbetingelser
    is Prismodell.AvtaltPrisPerUkesverk -> prisbetingelser
    is Prismodell.AvtaltPrisPerHeleUkesverk -> prisbetingelser
    is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker -> prisbetingelser
    Prismodell.ForhandsgodkjentPrisPerManedsverk -> null
}

fun Prismodell.satser(): List<AvtaltSats> = when (this) {
    is Prismodell.AnnenAvtaltPris,
    is Prismodell.ForhandsgodkjentPrisPerManedsverk,
    -> emptyList()

    is Prismodell.AvtaltPrisPerManedsverk -> toAvtalteSatser(satser)

    is Prismodell.AvtaltPrisPerUkesverk -> toAvtalteSatser(satser)

    is Prismodell.AvtaltPrisPerHeleUkesverk -> toAvtalteSatser(satser)

    is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker -> toAvtalteSatser(satser)
}

private fun toAvtalteSatser(satser: List<AvtaltSatsDto>): List<AvtaltSats> = satser.map {
    AvtaltSats(
        gjelderFra = it.gjelderFra,
        sats = it.pris,
    )
}

fun ArrangorDto.toDbo(kontaktpersoner: List<UUID>?): ArrangorDbo = ArrangorDbo(
    hovedenhet = id,
    underenheter = underenheter?.map { it.id } ?: emptyList(),
    kontaktpersoner = kontaktpersoner ?: emptyList(),
)

fun DetaljerRequest.toDbo(tiltakstypeId: UUID, arrangorDbo: ArrangorDbo?, status: AvtaleStatusType): DetaljerDbo = DetaljerDbo(
    navn = navn,
    status = status,
    sakarkivNummer = sakarkivNummer,
    tiltakstypeId = tiltakstypeId,
    arrangor = arrangorDbo,
    startDato = startDato,
    sluttDato = sluttDato,
    avtaletype = avtaletype,
    administratorer = administratorer,
    amoKategorisering = amoKategorisering,
    opsjonsmodell = opsjonsmodell,
    utdanningslop = utdanningslop,
)

fun PersonvernRequest.toDbo(): PersonvernDbo = PersonvernDbo(
    personvernBekreftet = personvernBekreftet,
    personopplysninger = personopplysninger,
)

fun VeilederinfoRequest.toDbo(): VeilederinformasjonDbo = VeilederinformasjonDbo(
    redaksjoneltInnhold = RedaksjoneltInnholdDbo(
        beskrivelse = beskrivelse,
        faneinnhold = faneinnhold,
    ),
    navEnheter = navEnheter.toSet(),
)
