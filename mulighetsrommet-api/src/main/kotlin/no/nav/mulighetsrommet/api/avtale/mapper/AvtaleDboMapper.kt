package no.nav.mulighetsrommet.api.avtale.mapper

import PersonvernDbo
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.avtale.api.DetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.PersonvernRequest
import no.nav.mulighetsrommet.api.avtale.api.VeilederinfoRequest
import no.nav.mulighetsrommet.api.avtale.db.ArrangorDbo
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.db.DetaljerDbo
import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.db.RedaksjoneltInnholdDbo
import no.nav.mulighetsrommet.api.avtale.db.VeilederinformasjonDbo
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSatsDto
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.model.AmoKategorisering
import no.nav.mulighetsrommet.model.AvtaleStatusType
import java.util.UUID

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
            id = avtale.prismodell.id,
            type = avtale.prismodell.type,
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
}

fun Prismodell.prisbetingelser(): String? = when (this) {
    is Prismodell.AnnenAvtaltPris -> prisbetingelser
    is Prismodell.AvtaltPrisPerManedsverk -> prisbetingelser
    is Prismodell.AvtaltPrisPerUkesverk -> prisbetingelser
    is Prismodell.AvtaltPrisPerHeleUkesverk -> prisbetingelser
    is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker -> prisbetingelser
    is Prismodell.ForhandsgodkjentPrisPerManedsverk -> null
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

fun DetaljerRequest.toDbo(
    tiltakstypeId: UUID,
    arrangorDbo: ArrangorDbo?,
    status: AvtaleStatusType,
    amoKategorisering: AmoKategorisering?,
): DetaljerDbo = DetaljerDbo(
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
