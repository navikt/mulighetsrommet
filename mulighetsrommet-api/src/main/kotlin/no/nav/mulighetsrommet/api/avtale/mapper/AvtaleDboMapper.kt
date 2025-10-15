package no.nav.mulighetsrommet.api.avtale.mapper

import PersonvernDbo
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
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.*

object AvtaleDboMapper {
    fun fromAvtale(avtale: Avtale) = AvtaleDbo(
        id = avtale.id,
        navn = avtale.navn,
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
        navEnheter = avtale.kontorstruktur.flatMap {
            it.kontorer.map { kontor -> kontor.enhetsnummer } + it.region.enhetsnummer
        }.toSet(),
        avtaletype = avtale.avtaletype,
        administratorer = avtale.administratorer.map { it.navIdent },
        beskrivelse = avtale.beskrivelse,
        faneinnhold = avtale.faneinnhold,
        personopplysninger = avtale.personopplysninger,
        personvernBekreftet = avtale.personvernBekreftet,
        amoKategorisering = avtale.amoKategorisering,
        opsjonsmodell = avtale.opsjonsmodell,
        utdanningslop = avtale.utdanningslop?.toDbo(),
        prismodell = avtale.prismodell.type,
        prisbetingelser = avtale.prismodell.prisbetingelser(),
        satser = avtale.prismodell.satser(),
    )

    fun fromValidatedAvtaleRequest(
        avtaleId: UUID,
        detaljerDbo: DetaljerDbo,
        prismodellDbo: PrismodellDbo,
        personvernDbo: PersonvernDbo,
        veilederinformasjonDbo: VeilederinformasjonDbo,
    ): AvtaleDbo = AvtaleDbo(
        id = avtaleId,
        navn = detaljerDbo.navn,
        sakarkivNummer = detaljerDbo.sakarkivNummer,
        tiltakstypeId = detaljerDbo.tiltakstypeId,
        arrangor = detaljerDbo.arrangor,
        startDato = detaljerDbo.startDato,
        sluttDato = detaljerDbo.sluttDato,
        status = detaljerDbo.status,
        avtaletype = detaljerDbo.avtaletype,
        administratorer = detaljerDbo.administratorer,
        navEnheter = veilederinformasjonDbo.navEnheter.toSet(),
        beskrivelse = veilederinformasjonDbo.redaksjoneltInnhold?.beskrivelse,
        faneinnhold = veilederinformasjonDbo.redaksjoneltInnhold?.faneinnhold,
        personopplysninger = personvernDbo.personopplysninger,
        personvernBekreftet = personvernDbo.personvernBekreftet,
        amoKategorisering = detaljerDbo.amoKategorisering,
        opsjonsmodell = detaljerDbo.opsjonsmodell,
        utdanningslop = detaljerDbo.utdanningslop,
        prismodell = prismodellDbo.prismodell,
        prisbetingelser = prismodellDbo.prisbetingelser,
        satser = prismodellDbo.satser,
    )

    fun toAvtaleRequest(dbo: AvtaleDbo, arrangor: DetaljerRequest.Arrangor?, tiltakskode: Tiltakskode) = AvtaleRequest(
        id = dbo.id,
        detaljer = DetaljerRequest(
            navn = dbo.navn,
            sakarkivNummer = dbo.sakarkivNummer,
            tiltakskode = tiltakskode,
            arrangor = arrangor,
            startDato = dbo.startDato,
            sluttDato = dbo.sluttDato,
            avtaletype = dbo.avtaletype,
            administratorer = dbo.administratorer,
            amoKategorisering = dbo.amoKategorisering,
            opsjonsmodell = dbo.opsjonsmodell,
            utdanningslop = dbo.utdanningslop,
        ),

        veilederinformasjon = VeilederinfoRequest(
            navEnheter = dbo.navEnheter.toList(),
            beskrivelse = dbo.beskrivelse,
            faneinnhold = dbo.faneinnhold,
        ),
        personvern = PersonvernRequest(
            personopplysninger = dbo.personopplysninger,
            personvernBekreftet = dbo.personvernBekreftet,
        ),
        prismodell = PrismodellRequest(
            type = dbo.prismodell,
            prisbetingelser = dbo.prisbetingelser,
            satser = dbo.satser.map {
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

fun DetaljerRequest.toDbo(tiltakstypeId: UUID, arrangor: ArrangorDbo?, status: AvtaleStatusType): DetaljerDbo = DetaljerDbo(
    navn = navn,
    status = status,
    sakarkivNummer = sakarkivNummer,
    tiltakstypeId = tiltakstypeId,
    arrangor = arrangor,
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

fun VeilederinfoRequest.toDbo(navenheter: Set<NavEnhetNummer>): VeilederinformasjonDbo = VeilederinformasjonDbo(
    redaksjoneltInnhold = RedaksjoneltInnholdDbo(
        beskrivelse = beskrivelse,
        faneinnhold = faneinnhold,
    ),
    navEnheter = navenheter,
)
