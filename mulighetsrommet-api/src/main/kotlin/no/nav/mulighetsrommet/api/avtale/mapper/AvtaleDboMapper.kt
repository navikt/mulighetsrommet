package no.nav.mulighetsrommet.api.avtale.mapper

import no.nav.mulighetsrommet.api.avtale.AvtaleRequest
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.model.AvtaleStatus

object AvtaleDboMapper {
    fun fromAvtaleRequest(
        request: AvtaleRequest,
        arrangor: AvtaleDbo.Arrangor?,
        status: AvtaleStatus,
    ): AvtaleDbo = AvtaleDbo(
        id = request.id,
        navn = request.navn,
        avtalenummer = request.avtalenummer,
        sakarkivNummer = request.sakarkivNummer,
        tiltakstypeId = request.tiltakstypeId,
        arrangor = arrangor,
        startDato = request.startDato,
        sluttDato = request.sluttDato,
        status = status,
        avtaletype = request.avtaletype,
        antallPlasser = null,
        administratorer = request.administratorer,
        prisbetingelser = request.prisbetingelser,
        navEnheter = request.navEnheter,
        beskrivelse = request.beskrivelse,
        faneinnhold = request.faneinnhold,
        personopplysninger = request.personopplysninger,
        personvernBekreftet = request.personvernBekreftet,
        amoKategorisering = request.amoKategorisering,
        opsjonsmodell = request.opsjonsmodell,
        utdanningslop = request.utdanningslop,
        prismodell = request.prismodell,
    )
}
