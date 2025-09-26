package no.nav.mulighetsrommet.api.avtale.mapper

import no.nav.mulighetsrommet.api.avtale.api.AvtaleDetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.AvtalePersonvernRequest
import no.nav.mulighetsrommet.api.avtale.api.AvtaleVeilederinfoRequest
import no.nav.mulighetsrommet.api.avtale.db.ArrangorDbo
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.db.DetaljerDbo
import no.nav.mulighetsrommet.api.avtale.db.OpsjonsmodellDbo
import no.nav.mulighetsrommet.api.avtale.db.PersonvernDbo
import no.nav.mulighetsrommet.api.avtale.db.PrismodellDbo
import no.nav.mulighetsrommet.api.avtale.db.RedaksjoneltInnholdDbo
import no.nav.mulighetsrommet.api.avtale.db.VeilederinformasjonDbo
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSatsDto
import no.nav.mulighetsrommet.api.avtale.model.Opsjonsmodell
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import java.util.UUID

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
        prismodell = PrismodellDbo(
            prismodell = avtale.prismodell.type,
            prisbetingelser = avtale.prismodell.prisbetingelser(),
            satser = avtale.prismodell.satser(),
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
        avtaleId: UUID,
        status: AvtaleStatusType,
        detaljerDbo: DetaljerDbo,
        prismodellDbo: PrismodellDbo,
        personvernDbo: PersonvernDbo,
        veilederinformasjonDbo: VeilederinformasjonDbo,
    ): AvtaleDbo = AvtaleDbo(
        id = avtaleId,
        status = status,
        detaljer = detaljerDbo,
        prismodell = prismodellDbo,
        veilederinformasjon = veilederinformasjonDbo,
        personvern = personvernDbo,
        avtalenummer = null,
    )

    fun AvtaleDetaljerRequest.toDbo(
        arrangor: ArrangorDbo?,
        tiltakstypeId: UUID,
    ): DetaljerDbo = DetaljerDbo(
        navn = navn,
        tiltakstypeId = tiltakstypeId,
        sakarkivnummer = sakarkivNummer?.value,
        startDato = startDato,
        sluttDato = sluttDato,
        avtaletype = avtaletype,
        administratorer = administratorer,
        arrangor = arrangor,
        opsjonsmodell = opsjonsmodell.toDbo(),
        amoKategorisering = amoKategorisering,
        utdanningslop = utdanningslop,
    )

    fun AvtalePersonvernRequest.toDbo(): PersonvernDbo = PersonvernDbo(
        personvernBekreftet = personvernBekreftet,
        personopplysninger = personopplysninger,
    )

    fun AvtaleVeilederinfoRequest.toDbo(navenheter: Set<NavEnhetNummer>): VeilederinformasjonDbo = VeilederinformasjonDbo(
        redaksjoneltInnhold = RedaksjoneltInnholdDbo(
            beskrivelse = beskrivelse,
            faneinnhold = faneinnhold,
        ),
        navEnheter = navenheter,
    )

    fun Opsjonsmodell.toDbo(): OpsjonsmodellDbo = OpsjonsmodellDbo(
        type = type,
        maksVarighet = maksVarighet,
        customNavn = customNavn,

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
