package no.nav.mulighetsrommet.api.utbetaling.model

import no.nav.mulighetsrommet.api.avtale.model.AvtaltSats
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.tilsagn.model.Tilsagn
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnBeregningFri
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import no.nav.mulighetsrommet.model.withValuta
import no.nav.tiltak.okonomi.BestillingStatusType
import java.time.Instant
import java.util.UUID

object BeregningTestHelpers {

    fun createGjennomforingForPrisPerManedsverk(
        id: UUID = UUID.randomUUID(),
        periode: Periode,
        satser: List<AvtaltSats>,
        stengt: List<GjennomforingAvtale.StengtPeriode> = emptyList(),
    ): GjennomforingAvtale = createGjennomforing(
        id = id,
        periode = periode,
        prismodell = Prismodell.AvtaltPrisPerManedsverk(
            id = UUID.randomUUID(),
            prisbetingelser = null,
            satser = satser,
            valuta = satser.first().sats.valuta,
        ),
        stengt = stengt,
    )

    fun createGjennomforingForPrisPerUkesverk(
        id: UUID = UUID.randomUUID(),
        periode: Periode,
        satser: List<AvtaltSats>,
        stengt: List<GjennomforingAvtale.StengtPeriode> = emptyList(),
    ): GjennomforingAvtale = createGjennomforing(
        id = id,
        periode = periode,
        prismodell = Prismodell.AvtaltPrisPerUkesverk(
            id = UUID.randomUUID(),
            prisbetingelser = null,
            satser = satser,
            valuta = satser.first().sats.valuta,
        ),
        stengt = stengt,
    )

    fun createGjennomforingForPrisPerHeleUkesverk(
        id: UUID = UUID.randomUUID(),
        periode: Periode,
        satser: List<AvtaltSats>,
        stengt: List<GjennomforingAvtale.StengtPeriode> = emptyList(),
    ): GjennomforingAvtale = createGjennomforing(
        id = id,
        periode = periode,
        prismodell = Prismodell.AvtaltPrisPerHeleUkesverk(
            id = UUID.randomUUID(),
            prisbetingelser = null,
            satser = satser,
            valuta = satser.first().sats.valuta,
        ),
        stengt = stengt,
    )

    fun createGjennomforingForForhandsgodkjentPris(
        id: UUID = UUID.randomUUID(),
        tiltakskode: Tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        periode: Periode,
        sats: ValutaBelop,
        stengt: List<GjennomforingAvtale.StengtPeriode> = emptyList(),
    ): GjennomforingAvtale = createGjennomforing(
        id = id,
        periode = periode,
        prismodell = Prismodell.ForhandsgodkjentPrisPerManedsverk(
            id = UUID.randomUUID(),
            satser = listOf(AvtaltSats(periode.start, sats)),
            valuta = sats.valuta,
        ),
        tiltakskode = tiltakskode,
        stengt = stengt,
    )

    fun createGjennomforingForForhandsgodkjentSatsPerAvtaltTiltaksplass(
        id: UUID = UUID.randomUUID(),
        tiltakskode: Tiltakskode = Tiltakskode.TILRETTELAGT_ARBEID_ORDINAER,
        periode: Periode,
        sats: ValutaBelop = ValutaBelop(7_321, Valuta.NOK),
    ): GjennomforingAvtale = createGjennomforing(
        id = id,
        periode = periode,
        prismodell = Prismodell.ForhandsgodkjentPrisPerAvtaltTiltaksplass(
            id = UUID.randomUUID(),
            satser = listOf(AvtaltSats(periode.start, sats)),
            valuta = sats.valuta,
        ),
        tiltakskode = tiltakskode,
        stengt = emptyList(),
    )

    fun createTilsagn(
        gjennomforing: GjennomforingAvtale,
        periode: Periode,
        belop: ValutaBelop,
    ): Tilsagn = Tilsagn(
        id = UUID.randomUUID(),
        type = TilsagnType.TILSAGN,
        periode = periode,
        belopBrukt = 0.withValuta(belop.valuta),
        kostnadssted = NavEnhet(
            navn = "Test enhet",
            enhetsnummer = NavEnhetNummer("0300"),
            status = NavEnhetStatus.AKTIV,
            type = NavEnhetType.FYLKE,
        ),
        beregning = TilsagnBeregningFri(
            input = TilsagnBeregningFri.Input(
                listOf(
                    TilsagnBeregningFri.InputLinje(
                        id = UUID.randomUUID(),
                        beskrivelse = "",
                        pris = belop,
                        antall = 1,
                    ),
                ),
                prisbetingelser = null,
            ),
            output = TilsagnBeregningFri.Output(pris = belop),
        ),
        lopenummer = 1,
        bestilling = Tilsagn.Bestilling(
            bestillingsnummer = "TEST-1",
            status = BestillingStatusType.AKTIV,
        ),
        tiltakstype = Tilsagn.Tiltakstype(
            tiltakskode = gjennomforing.tiltakstype.tiltakskode,
            navn = gjennomforing.tiltakstype.navn,
        ),
        gjennomforing = Tilsagn.Gjennomforing(
            id = gjennomforing.id,
            lopenummer = gjennomforing.lopenummer,
            navn = gjennomforing.navn,
        ),
        arrangor = Tilsagn.Arrangor(
            id = gjennomforing.arrangor.id,
            organisasjonsnummer = gjennomforing.arrangor.organisasjonsnummer,
            navn = gjennomforing.arrangor.navn,
            slettet = gjennomforing.arrangor.slettet,
        ),
        status = TilsagnStatus.GODKJENT,
        kommentar = null,
        beskrivelse = null,
        journalpost = null,
        deltakere = emptyList(),
    )

    private fun createGjennomforing(
        id: UUID,
        periode: Periode,
        prismodell: Prismodell,
        tiltakskode: Tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        stengt: List<GjennomforingAvtale.StengtPeriode>,
    ): GjennomforingAvtale {
        val now = Instant.now()
        return GjennomforingAvtale(
            id = id,
            tiltakstype = Gjennomforing.Tiltakstype(
                id = UUID.randomUUID(),
                navn = "Test tiltakstype",
                tiltakskode = tiltakskode,
            ),
            lopenummer = Tiltaksnummer("2025/1"),
            arrangor = Gjennomforing.ArrangorUnderenhet(
                id = UUID.randomUUID(),
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                navn = "Test arrangør",
                slettet = false,
            ),
            arena = null,
            navn = "Test gjennomføring",
            startDato = periode.start,
            sluttDato = periode.getLastInclusiveDate(),
            deltidsprosent = 100.0,
            antallPlasser = 10,
            status = GjennomforingStatusType.GJENNOMFORES,
            apentForPamelding = true,
            avtaleId = UUID.randomUUID(),
            kontorstruktur = emptyList(),
            oppstart = GjennomforingOppstartstype.LOPENDE,
            pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
            opprettetTidspunkt = now,
            oppdatertTidspunkt = now,
            stengt = stengt,
            prismodell = prismodell,
        )
    }

    fun createDeltaker(
        periode: Periode,
        deltakelsesmengder: List<Deltakelsesmengde> = emptyList(),
        status: DeltakerStatusType = DeltakerStatusType.DELTAR,
    ): Deltaker = Deltaker(
        id = UUID.randomUUID(),
        gjennomforingId = UUID.randomUUID(),
        startDato = periode.start,
        sluttDato = periode.getLastInclusiveDate(),
        registrertTidspunkt = periode.start.atStartOfDay(),
        endretTidspunkt = periode.start.atStartOfDay(),
        status = DeltakerStatus(
            type = status,
            aarsak = null,
            opprettetTidspunkt = periode.start.atStartOfDay(),
        ),
        deltakelsesmengder = deltakelsesmengder,
        innholdAnnet = null,
        navVeileder = null,
    )

    fun toStengtPeriode(periode: Periode, beskrivelse: String = "Stengt"): GjennomforingAvtale.StengtPeriode {
        return GjennomforingAvtale.StengtPeriode(
            id = 1,
            start = periode.start,
            slutt = periode.getLastInclusiveDate(),
            beskrivelse = beskrivelse,
        )
    }
}
