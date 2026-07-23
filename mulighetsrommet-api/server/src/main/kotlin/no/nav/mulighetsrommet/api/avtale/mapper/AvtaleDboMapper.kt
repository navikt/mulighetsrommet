package no.nav.mulighetsrommet.api.avtale.mapper

import no.nav.mulighetsrommet.api.avtale.AvtaleValidator
import no.nav.mulighetsrommet.api.avtale.api.DetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.PersonvernRequest
import no.nav.mulighetsrommet.api.domain.opplaring.OpplaringKategorisering
import no.nav.mulighetsrommet.api.domain.tiltak.Avtale
import no.nav.mulighetsrommet.api.persistence.tiltak.AvtaleArrangorDbo
import no.nav.mulighetsrommet.api.persistence.tiltak.AvtaleDbo
import no.nav.mulighetsrommet.api.persistence.tiltak.DetaljerDbo
import no.nav.mulighetsrommet.api.persistence.tiltak.PersonvernDbo
import no.nav.mulighetsrommet.api.persistence.tiltak.RedaksjoneltInnholdDbo
import no.nav.mulighetsrommet.api.persistence.tiltak.VeilederinformasjonDbo
import no.nav.mulighetsrommet.model.AvtaleStatusType
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

object AvtaleDboMapper {
    fun fromAvtale(avtale: Avtale): AvtaleDbo = AvtaleDbo(
        id = avtale.id,
        detaljerDbo = DetaljerDbo(
            navn = avtale.navn,
            avtaletype = avtale.avtaletype,
            tiltakskode = avtale.tiltakstype.tiltakskode,
            sakarkivNummer = avtale.sakarkivNummer,
            arrangor = avtale.arrangor?.let { arrangor ->
                AvtaleArrangorDbo(
                    hovedenhet = arrangor.id,
                    underenheter = arrangor.underenheter.map { it.id },
                    kontaktpersoner = arrangor.kontaktpersoner.map { it.id },
                )
            },
            startDato = avtale.startDato,
            sluttDato = avtale.sluttDato,
            status = avtale.status.type,
            opplaringKategorisering = avtale.opplaring,
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
            navEnheter = avtale.navEnheter,
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

fun AvtaleValidator.Ctx.AvtaleArrangor.toDbo(kontaktpersoner: List<UUID>?): AvtaleArrangorDbo = AvtaleArrangorDbo(
    hovedenhet = hovedenhet.id,
    underenheter = underenheter.map { it.id },
    kontaktpersoner = kontaktpersoner ?: emptyList(),
)

fun DetaljerRequest.toDbo(
    tiltakskode: Tiltakskode,
    arrangorDbo: AvtaleArrangorDbo?,
    status: AvtaleStatusType,
    kategorisering: OpplaringKategorisering?,
): DetaljerDbo = DetaljerDbo(
    navn = navn,
    status = status,
    sakarkivNummer = sakarkivNummer,
    tiltakskode = tiltakskode,
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
