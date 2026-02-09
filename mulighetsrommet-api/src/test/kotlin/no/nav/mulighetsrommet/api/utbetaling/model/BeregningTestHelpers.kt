package no.nav.mulighetsrommet.api.utbetaling.model

import no.nav.mulighetsrommet.api.avtale.model.AvtaltSatsDto
import no.nav.mulighetsrommet.api.avtale.model.Prismodell
import no.nav.mulighetsrommet.api.gjennomforing.model.AvtaleGjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingStatus
import no.nav.mulighetsrommet.arena.ArenaMigrering
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.Tiltaksnummer
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

object BeregningTestHelpers {

    fun createGjennomforingForPrisPerManedsverk(
        id: UUID = UUID.randomUUID(),
        periode: Periode,
        satser: List<AvtaltSatsDto>,
        stengt: List<AvtaleGjennomforing.StengtPeriode> = emptyList(),
        valuta: Valuta = Valuta.NOK,
    ): AvtaleGjennomforing = createGjennomforing(
        id = id,
        periode = periode,
        prismodell = Prismodell.AvtaltPrisPerManedsverk(
            id = UUID.randomUUID(),
            prisbetingelser = null,
            satser = satser,
            valuta = valuta,
        ),
        stengt = stengt,
    )

    fun createGjennomforingForPrisPerUkesverk(
        id: UUID = UUID.randomUUID(),
        periode: Periode,
        satser: List<AvtaltSatsDto>,
        stengt: List<AvtaleGjennomforing.StengtPeriode> = emptyList(),
        valuta: Valuta = Valuta.NOK,
    ): AvtaleGjennomforing = createGjennomforing(
        id = id,
        periode = periode,
        prismodell = Prismodell.AvtaltPrisPerUkesverk(
            id = UUID.randomUUID(),
            prisbetingelser = null,
            satser = satser,
            valuta = valuta,
        ),
        stengt = stengt,
    )

    fun createGjennomforingForPrisPerHeleUkesverk(
        id: UUID = UUID.randomUUID(),
        periode: Periode,
        satser: List<AvtaltSatsDto>,
        stengt: List<AvtaleGjennomforing.StengtPeriode> = emptyList(),
        valuta: Valuta = Valuta.NOK,
    ): AvtaleGjennomforing = createGjennomforing(
        id = id,
        periode = periode,
        prismodell = Prismodell.AvtaltPrisPerHeleUkesverk(
            id = UUID.randomUUID(),
            prisbetingelser = null,
            satser = satser,
            valuta = valuta,
        ),
        stengt = stengt,
    )

    fun createGjennomforingForForhandsgodkjentPris(
        id: UUID = UUID.randomUUID(),
        periode: Periode,
        sats: ValutaBelop,
        stengt: List<AvtaleGjennomforing.StengtPeriode> = emptyList(),
        valuta: Valuta = Valuta.NOK,
    ): AvtaleGjennomforing = createGjennomforing(
        id = id,
        periode = periode,
        prismodell = Prismodell.ForhandsgodkjentPrisPerManedsverk(
            id = UUID.randomUUID(),
            satser = listOf(AvtaltSatsDto(periode.start, sats)),
            valuta = valuta,
        ),
        tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        stengt = stengt,
    )

    private fun createGjennomforing(
        id: UUID,
        periode: Periode,
        prismodell: Prismodell,
        tiltakskode: Tiltakskode = Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
        stengt: List<AvtaleGjennomforing.StengtPeriode>,
    ): AvtaleGjennomforing {
        val now = Instant.now()
        return AvtaleGjennomforing(
            id = id,
            tiltakstype = no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing.Tiltakstype(
                id = UUID.randomUUID(),
                navn = "Test tiltakstype",
                tiltakskode = tiltakskode,
            ),
            lopenummer = Tiltaksnummer("2025/1"),
            arrangor = no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing.ArrangorUnderenhet(
                id = UUID.randomUUID(),
                organisasjonsnummer = Organisasjonsnummer("123456789"),
                navn = "Test arrangør",
                kontaktpersoner = emptyList(),
                slettet = false,
            ),
            arena = null,
            navn = "Test gjennomføring",
            startDato = periode.start,
            sluttDato = periode.getLastInclusiveDate(),
            deltidsprosent = 100.0,
            antallPlasser = 10,
            opphav = ArenaMigrering.Opphav.TILTAKSADMINISTRASJON,
            status = GjennomforingStatus.Gjennomfores,
            apentForPamelding = true,
            avtaleId = UUID.randomUUID(),
            administratorer = emptyList(),
            kontorstruktur = emptyList(),
            oppstart = GjennomforingOppstartstype.LOPENDE,
            pameldingType = GjennomforingPameldingType.DIREKTE_VEDTAK,
            kontaktpersoner = emptyList(),
            oppmoteSted = null,
            faneinnhold = null,
            beskrivelse = null,
            opprettetTidspunkt = now,
            oppdatertTidspunkt = now,
            publisert = true,
            estimertVentetid = null,
            tilgjengeligForArrangorDato = null,
            amoKategorisering = null,
            utdanningslop = null,
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
        registrertDato = periode.start,
        endretTidspunkt = periode.start.atStartOfDay(),
        status = DeltakerStatus(
            type = status,
            aarsak = null,
            opprettetDato = periode.start.atStartOfDay(),
        ),
        deltakelsesmengder = deltakelsesmengder,
    )

    fun toStengtPeriode(periode: Periode, beskrivelse: String = "Stengt"): AvtaleGjennomforing.StengtPeriode {
        return AvtaleGjennomforing.StengtPeriode(
            id = 1,
            start = periode.start,
            slutt = periode.getLastInclusiveDate(),
            beskrivelse = beskrivelse,
        )
    }

    fun toAvtaltSats(gjelderFra: LocalDate, pris: ValutaBelop): AvtaltSatsDto {
        return AvtaltSatsDto(gjelderFra = gjelderFra, pris = pris)
    }
}
