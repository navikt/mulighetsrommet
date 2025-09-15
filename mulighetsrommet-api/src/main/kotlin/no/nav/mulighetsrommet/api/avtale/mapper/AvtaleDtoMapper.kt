package no.nav.mulighetsrommet.api.avtale.mapper

import no.nav.mulighetsrommet.api.avtale.model.Avtale
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto

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
        status = avtale.status,
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
        prismodell = avtale.prismodell,
    )
}
