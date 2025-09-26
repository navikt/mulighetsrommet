package no.nav.mulighetsrommet.api.avtale.mapper

import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.avtale.model.AvtaleStatus
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.avtale.model.toDto
import no.nav.mulighetsrommet.api.tilsagn.model.AvtalteSatser
import no.nav.mulighetsrommet.model.DataElement

object AvtaleDtoMapper {
    fun fromAvtale(avtale: Avtale) = AvtaleDto(
        id = avtale.id,
        tiltakstype = avtale.tiltakstype,
        navn = avtale.navn,
        avtalenummer = avtale.avtalenummer,
        sakarkivNummer = avtale.sakarkivNummer,
        arrangor = avtale.arrangor,
        startDato = avtale.startDato,
        sluttDato = avtale.sluttDato,
        arenaAnsvarligEnhet = avtale.arenaAnsvarligEnhet,
        avtaletype = avtale.avtaletype,
        status = fromAvtaleStatus(avtale.status),
        administratorer = avtale.administratorer,
        opphav = avtale.opphav,
        kontorstruktur = avtale.kontorstruktur,
        beskrivelse = avtale.beskrivelse,
        faneinnhold = avtale.faneinnhold,
        personopplysninger = avtale.personopplysninger,
        personvernBekreftet = avtale.personvernBekreftet,
        amoKategorisering = avtale.amoKategorisering,
        opsjonsmodell = avtale.opsjonsmodell,
        opsjonerRegistrert = avtale.opsjonerRegistrert,
        utdanningslop = avtale.utdanningslop,
        prismodell = fromPrismodell(avtale),
    )

    private fun fromAvtaleStatus(status: AvtaleStatus): AvtaleDto.Status {
        val variant = when (status) {
            AvtaleStatus.Utkast, AvtaleStatus.Avsluttet -> DataElement.Status.Variant.NEUTRAL
            AvtaleStatus.Aktiv -> DataElement.Status.Variant.SUCCESS
            is AvtaleStatus.Avbrutt -> DataElement.Status.Variant.ERROR
        }
        val description = when (status) {
            AvtaleStatus.Utkast, AvtaleStatus.Avsluttet, AvtaleStatus.Aktiv -> null
            is AvtaleStatus.Avbrutt -> status.forklaring
        }
        val element = DataElement.Status(status.type.beskrivelse, variant, description)
        return AvtaleDto.Status(status.type, element)
    }

    private fun fromPrismodell(avtale: Avtale): AvtaleDto.Prismodell {
        val prismodell = avtale.prismodell

        val satser = when (prismodell) {
            is Prismodell.AnnenAvtaltPris -> null

            is Prismodell.ForhandsgodkjentPrisPerManedsverk,
            -> AvtalteSatser.getForhandsgodkjenteSatser(avtale.tiltakstype.tiltakskode).toDto()

            is Prismodell.AvtaltPrisPerManedsverk -> prismodell.satser
            is Prismodell.AvtaltPrisPerUkesverk -> prismodell.satser
            is Prismodell.AvtaltPrisPerHeleUkesverk -> prismodell.satser
            is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker -> prismodell.satser
        }
        val prisbetingelser = when (prismodell) {
            Prismodell.ForhandsgodkjentPrisPerManedsverk,
            -> null

            is Prismodell.AnnenAvtaltPris -> prismodell.prisbetingelser
            is Prismodell.AvtaltPrisPerManedsverk -> prismodell.prisbetingelser
            is Prismodell.AvtaltPrisPerUkesverk -> prismodell.prisbetingelser
            is Prismodell.AvtaltPrisPerHeleUkesverk -> prismodell.prisbetingelser
            is Prismodell.AvtaltPrisPerTimeOppfolgingPerDeltaker -> prismodell.prisbetingelser
        }
        return AvtaleDto.Prismodell(
            type = prismodell.type,
            satser = satser,
            prisbetingelser = prisbetingelser,
        )
    }
}
