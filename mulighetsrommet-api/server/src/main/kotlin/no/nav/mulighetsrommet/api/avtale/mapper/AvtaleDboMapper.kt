package no.nav.mulighetsrommet.api.avtale.mapper

import no.nav.mulighetsrommet.api.amo.db.OpplaringKategoriseringDbo
import no.nav.mulighetsrommet.api.amo.toDbo
import no.nav.mulighetsrommet.api.avtale.AvtaleValidator
import no.nav.mulighetsrommet.api.avtale.api.DetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.PersonvernRequest
import no.nav.mulighetsrommet.api.avtale.db.AvtaleArrangorDbo
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.db.DetaljerDbo
import no.nav.mulighetsrommet.api.avtale.db.PersonvernDbo
import no.nav.mulighetsrommet.api.avtale.db.RedaksjoneltInnholdDbo
import no.nav.mulighetsrommet.api.avtale.db.VeilederinformasjonDbo
import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Personopplysning
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
                AvtaleArrangorDbo(
                    hovedenhet = it,
                    underenheter = avtale.arrangor.underenheter.map { it.id },
                    kontaktpersoner = avtale.arrangor.kontaktpersoner.map { it.id },
                )
            },
            startDato = avtale.startDato,
            sluttDato = avtale.sluttDato,
            status = avtale.status.type,
            opplaringKategorisering = avtale.opplaringKategorisering?.toDbo(),
            opsjonsmodell = avtale.opsjonsmodell,
            administratorer = avtale.administratorer.map { it.navIdent },
        ),
        personvernDbo = PersonvernDbo(
            personopplysninger = avtale.personopplysninger
                .filter { it.type != Personopplysning.Type.ANNET }
                .map { it.type },
            annetChecked = avtale.personopplysninger.any { it.type == Personopplysning.Type.ANNET },
            annetBeskrivelse = avtale.personopplysninger.find { it.type == Personopplysning.Type.ANNET }?.beskrivelse,
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
        prismodeller = avtale.prismodeller.map { it.id },
    )

    fun fromValidatedAvtaleRequest(
        avtaleId: UUID,
        detaljerDbo: DetaljerDbo,
        prismodeller: List<UUID>,
        personvernDbo: PersonvernDbo,
        veilederinformasjonDbo: VeilederinformasjonDbo,
    ): AvtaleDbo = AvtaleDbo(
        id = avtaleId,
        detaljerDbo = detaljerDbo,
        prismodeller = prismodeller,
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
    is Prismodell.ForhandsgodkjentPrisPerAvtaltTiltaksplass -> null
    is Prismodell.TilskuddTilOpplaering -> tilleggsopplysninger
    is Prismodell.IngenKostnader -> tilleggsopplysninger
}

fun Prismodell.satser(): List<AvtaltSats> = when (this) {
    is Prismodell.AnnenAvtaltPris -> emptyList()
    is Prismodell.AvtaltPrisPerManedsverk -> satser
    is Prismodell.AvtaltPrisPerUkesverk -> satser
    is Prismodell.AvtaltPrisPerHeleUkesverk -> satser
    is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker -> satser
    is Prismodell.ForhandsgodkjentPrisPerManedsverk -> satser
    is Prismodell.ForhandsgodkjentPrisPerAvtaltTiltaksplass -> satser
    is Prismodell.TilskuddTilOpplaering -> listOf()
    is Prismodell.IngenKostnader -> listOf()
}

fun AvtaleValidator.Ctx.AvtaleArrangor.toDbo(kontaktpersoner: List<UUID>?): AvtaleArrangorDbo = AvtaleArrangorDbo(
    hovedenhet = hovedenhet.id,
    underenheter = underenheter.map { it.id },
    kontaktpersoner = kontaktpersoner ?: emptyList(),
)

fun DetaljerRequest.toDbo(
    tiltakstypeId: UUID,
    arrangorDbo: AvtaleArrangorDbo?,
    status: AvtaleStatusType,
    kategorisering: OpplaringKategoriseringDbo?,
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
    opplaringKategorisering = kategorisering,
    opsjonsmodell = opsjonsmodell,
)

fun PersonvernRequest.toDbo(): PersonvernDbo = PersonvernDbo(
    personvernBekreftet = personvernBekreftet,
    personopplysninger = personopplysninger,
    annetChecked = annetChecked ?: false,
    annetBeskrivelse = annetBeskrivelse,
)
