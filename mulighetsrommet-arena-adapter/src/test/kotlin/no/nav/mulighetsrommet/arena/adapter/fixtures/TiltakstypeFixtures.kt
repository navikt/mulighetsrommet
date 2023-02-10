package no.nav.mulighetsrommet.arena.adapter.fixtures

import no.nav.mulighetsrommet.arena.adapter.models.arena.Administrasjonskode
import no.nav.mulighetsrommet.arena.adapter.models.arena.Handlingsplan
import no.nav.mulighetsrommet.arena.adapter.models.arena.Rammeavtale
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltakstype
import java.time.LocalDateTime
import java.util.*

object TiltakstypeFixtures {
    val Gruppe = Tiltakstype(
        id = UUID.randomUUID(),
        navn = "Oppfølging",
        tiltakskode = "INDOPPFAG",
        rettPaaTiltakspenger = true,
        fraDato = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
        tilDato = LocalDateTime.of(2023, 1, 12, 0, 0, 0),
        tiltaksgruppekode = "tiltaksgruppekode",
        administrasjonskode = Administrasjonskode.AMO,
        sendTilsagnsbrevTilDeltaker = true,
        tiltakstypeSkalHaAnskaffelsesprosess = false,
        maksAntallPlasser = 10,
        maksAntallSokere = 10,
        harFastAntallPlasser = true,
        skalSjekkeAntallDeltakere = true,
        visLonnstilskuddskalkulator = false,
        rammeavtale = Rammeavtale.SKAL,
        opplaeringsgruppe = "opplaeringsgruppe",
        handlingsplan = Handlingsplan.AKT,
        tiltaksgjennomforingKreverSluttdato = true,
        maksPeriodeIMnd = 6,
        tiltaksgjennomforingKreverMeldeplikt = false,
        tiltaksgjennomforingKreverVedtak = false,
        tiltaksgjennomforingReservertForIABedrift = false,
        harRettPaaTilleggsstonader = false,
        harRettPaaUtdanning = false,
        tiltaksgjennomforingGenererTilsagnsbrevAutomatisk = false,
        visBegrunnelseForInnsoking = false,
        sendHenvisningsbrevOgHovedbrevTilArbeidsgiver = false,
        sendKopibrevOgHovedbrevTilArbeidsgiver = false,
        registrertIArenaDato = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
        sistEndretIArenaDato = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
    )

    val Individuell = Tiltakstype(
        id = UUID.randomUUID(),
        navn = "AMO",
        tiltakskode = "AMO",
        rettPaaTiltakspenger = false,
        fraDato = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
        tilDato = LocalDateTime.of(2023, 1, 12, 0, 0, 0),
        tiltaksgruppekode = "tiltaksgruppekode",
        administrasjonskode = Administrasjonskode.AMO,
        sendTilsagnsbrevTilDeltaker = true,
        tiltakstypeSkalHaAnskaffelsesprosess = false,
        maksAntallPlasser = 10,
        maksAntallSokere = 10,
        harFastAntallPlasser = true,
        skalSjekkeAntallDeltakere = true,
        visLonnstilskuddskalkulator = false,
        rammeavtale = Rammeavtale.SKAL,
        opplaeringsgruppe = "opplaeringsgruppe",
        handlingsplan = Handlingsplan.AKT,
        tiltaksgjennomforingKreverSluttdato = true,
        maksPeriodeIMnd = 6,
        tiltaksgjennomforingKreverMeldeplikt = false,
        tiltaksgjennomforingKreverVedtak = false,
        tiltaksgjennomforingReservertForIABedrift = false,
        harRettPaaTilleggsstonader = false,
        harRettPaaUtdanning = false,
        tiltaksgjennomforingGenererTilsagnsbrevAutomatisk = false,
        visBegrunnelseForInnsoking = false,
        sendHenvisningsbrevOgHovedbrevTilArbeidsgiver = false,
        sendKopibrevOgHovedbrevTilArbeidsgiver = false,
        registrertIArenaDato = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
        sistEndretIArenaDato = LocalDateTime.of(2023, 1, 11, 0, 0, 0),
    )
}
