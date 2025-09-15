import {
  Besluttelse,
  DataElementStatusVariant,
  DelutbetalingStatus,
  TilsagnStatus,
  TilsagnType,
  Tilskuddstype,
  UtbetalingBeregningDto,
  UtbetalingDto,
  UtbetalingKompaktDto,
  UtbetalingLinje,
  UtbetalingStatusDtoType,
  UtbetalingTypeDto,
  UtbetalingLinjeHandling,
} from "@tiltaksadministrasjon/api-client";
import { mockEnheter } from "./mock_enheter";

const utbetalingType: Record<"KORRIGERING" | "INVESTERING" | "INNSENDING", UtbetalingTypeDto> = {
  KORRIGERING: {
    displayName: "Korrigering",
    displayNameLong: null,
    tagName: "KOR",
  },
  INVESTERING: {
    displayName: "Korrigering",
    displayNameLong: "Utbetaling for investering",
    tagName: "INV",
  },
  INNSENDING: {
    displayName: "Innsending",
    displayNameLong: null,
    tagName: null,
  },
};

export const mockUtbetalinger: UtbetalingDto[] = [
  {
    id: "123e4567-e89b-12d3-a456-426614174000",
    periode: {
      start: "2024-01-01",
      slutt: "2024-06-30",
    },
    status: {
      type: UtbetalingStatusDtoType.VENTER_PA_ARRANGOR,
      status: { value: "Venter på arrangør", variant: DataElementStatusVariant.ALT },
    },
    createdAt: "2024-01-01T10:00:00",
    godkjentAvArrangorTidspunkt: null,
    belop: 15000,
    betalingsinformasjon: {
      kontonummer: "1234.56.78900",
      kid: "12345678901",
    },
    beskrivelse: "Utbetaling for første halvår 2024",
    innsendtAv: "Z123456",
    journalpostId: "JP123456",
    tilskuddstype: Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
    type: utbetalingType.INNSENDING,
    begrunnelseMindreBetalt: null,
  },
  {
    id: "123e4567-e89b-12d3-a456-426614174001",
    periode: {
      start: "2025-01-01",
      slutt: "2025-06-30",
    },
    status: {
      type: UtbetalingStatusDtoType.TIL_ATTESTERING,
      status: { value: "Til attestering", variant: DataElementStatusVariant.WARNING },
    },
    createdAt: "2024-07-01T14:30:00",
    godkjentAvArrangorTidspunkt: "2024-07-02T09:15:00",
    belop: 18000,
    betalingsinformasjon: {
      kontonummer: "9876.54.32100",
      kid: "98765432109",
    },
    beskrivelse: "Utbetaling for andre halvår 2024",
    innsendtAv: "Arrangør",
    journalpostId: "JP123457",
    tilskuddstype: Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
    type: utbetalingType.INNSENDING,
    begrunnelseMindreBetalt: null,
  },
  {
    id: "123e4567-e89b-12d3-a456-426614174002",
    periode: {
      start: "2025-01-01",
      slutt: "2025-03-31",
    },
    status: {
      type: UtbetalingStatusDtoType.RETURNERT,
      status: { value: "Returnert", variant: DataElementStatusVariant.ERROR },
    },
    createdAt: "2025-01-01T08:00:00",
    godkjentAvArrangorTidspunkt: null,
    belop: 9000,
    betalingsinformasjon: {
      kontonummer: "1111.22.33333",
      kid: "11122233344",
    },
    beskrivelse: "Utbetaling for første kvartal 2025",
    innsendtAv: "Z987654",
    journalpostId: "JP123458",
    tilskuddstype: Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
    type: utbetalingType.INNSENDING,
    begrunnelseMindreBetalt: null,
  },
  {
    id: "129e4567-e89b-12d3-a456-426614174002",
    periode: {
      start: "2025-03-01",
      slutt: "2025-03-31",
    },
    status: {
      type: UtbetalingStatusDtoType.OVERFORT_TIL_UTBETALING,
      status: { value: "Overført til utbetaling", variant: DataElementStatusVariant.SUCCESS },
    },
    createdAt: "2025-01-01T08:00:00",
    godkjentAvArrangorTidspunkt: null,
    belop: 9000,
    betalingsinformasjon: {
      kontonummer: "1111.22.33333",
      kid: "11122233344",
    },
    beskrivelse: "Utbetaling for første kvartal 2025",
    innsendtAv: "Z987654",
    journalpostId: "JP123458",
    tilskuddstype: Tilskuddstype.TILTAK_DRIFTSTILSKUDD,
    type: utbetalingType.INNSENDING,
    begrunnelseMindreBetalt: null,
  },
];

export const mockUtbetalingerKompakt: UtbetalingKompaktDto[] = [
  {
    id: "123e4567-e89b-12d3-a456-426614174000",
    periode: {
      start: "2024-01-01",
      slutt: "2024-06-30",
    },
    status: {
      type: UtbetalingStatusDtoType.VENTER_PA_ARRANGOR,
      status: { value: "Venter på arrangør", variant: DataElementStatusVariant.ALT },
    },
    belopUtbetalt: null,
    kostnadssteder: [mockEnheter._0105, mockEnheter._0106],
    type: utbetalingType.INNSENDING,
  },
  {
    kostnadssteder: [mockEnheter._0105, mockEnheter._0106],
    id: "123e4567-e89b-12d3-a456-426614174001",
    periode: {
      start: "2025-01-01",
      slutt: "2025-06-30",
    },
    status: {
      type: UtbetalingStatusDtoType.TIL_ATTESTERING,
      status: { value: "Til attestering", variant: DataElementStatusVariant.WARNING },
    },
    belopUtbetalt: null,
    type: utbetalingType.INNSENDING,
  },
  {
    id: "129e4567-e89b-12d3-a456-426614174002",
    periode: {
      start: "2025-03-01",
      slutt: "2025-03-31",
    },
    kostnadssteder: [mockEnheter._0105, mockEnheter._0106],
    belopUtbetalt: 13400,
    status: {
      type: UtbetalingStatusDtoType.OVERFORT_TIL_UTBETALING,
      status: { value: "Overført til utbetaling", variant: DataElementStatusVariant.SUCCESS },
    },
    type: utbetalingType.INNSENDING,
  },
  {
    id: "123e4567-e89b-12d3-a456-426614174002",
    periode: {
      start: "2025-01-01",
      slutt: "2025-03-31",
    },
    status: {
      type: UtbetalingStatusDtoType.RETURNERT,
      status: { value: "Returnert", variant: DataElementStatusVariant.ERROR },
    },
    kostnadssteder: [mockEnheter._0105, mockEnheter._0106],
    belopUtbetalt: null,
    type: utbetalingType.INNSENDING,
  },
];

// Mock data for UtbetalingLinje
export const mockUtbetalingLinjer: UtbetalingLinje[] = [
  {
    id: "456e4567-e89b-12d3-a456-426614174000",
    tilsagn: {
      id: "10e393b0-1b7c-4c68-9a42-b541b2f114b8",
      type: TilsagnType.TILSAGN,
      periode: {
        start: "2024-01-01",
        slutt: "2024-06-30",
      },
      kostnadssted: {
        enhetsnummer: "0300",
        navn: "Nav Oslo",
      },
      belop: 14_000,
      belopBrukt: 4_000,
      belopGjenstaende: 10_000,
      status: {
        type: TilsagnStatus.TIL_GODKJENNING,
        status: { value: "Til godkjenning", variant: DataElementStatusVariant.WARNING },
      },
      bestillingsnummer: "A-2024/123",
      kommentar: "Min kommentar",
    },
    status: {
      type: DelutbetalingStatus.TIL_ATTESTERING,
      status: { value: "Til godkjenning", variant: DataElementStatusVariant.WARNING },
    },
    belop: 5000,
    gjorOppTilsagn: true,
    opprettelse: {
      type: "no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto.TilBeslutning",
      behandletAv: {
        navn: "Bertil Bengtson",
      },
      behandletTidspunkt: "2024-01-01T22:00:00",
      aarsaker: ["Utbetaling for første halvår 2024"],
      forklaring: "Utbetaling for tilsagn",
    },
    handlinger: [UtbetalingLinjeHandling.ATTESTER, UtbetalingLinjeHandling.RETURNER],
  },
  {
    handlinger: [UtbetalingLinjeHandling.ATTESTER, UtbetalingLinjeHandling.RETURNER],
    id: "456e4567-e89b-12d3-a456-426614174001",
    tilsagn: {
      id: "fd1825aa-1951-4de2-9b72-12d22f121e92",
      type: TilsagnType.TILSAGN,
      periode: {
        start: "2024-07-01",
        slutt: "2024-12-31",
      },
      kostnadssted: {
        enhetsnummer: "0300",
        navn: "Nav Oslo",
      },
      belop: 14_000,
      belopBrukt: 4_000,
      belopGjenstaende: 10_000,
      status: {
        type: TilsagnStatus.TIL_ANNULLERING,
        status: { value: "Til annullering", variant: DataElementStatusVariant.ERROR_BORDER },
      },
      bestillingsnummer: "A-2024/123",
      kommentar: "Min kommentar",
    },
    status: {
      type: DelutbetalingStatus.RETURNERT,
      status: { value: "Returnert", variant: DataElementStatusVariant.ERROR },
    },
    belop: 7500,
    gjorOppTilsagn: false,
    opprettelse: {
      type: "no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto.Besluttet",
      behandletAv: {
        navn: "Bertil Bengtson",
      },
      behandletTidspunkt: "2024-01-01T22:00:00",
      besluttetAv: {
        navn: "Per Haraldsen",
      },
      besluttetTidspunkt: "2024-01-02T10:00:00",
      aarsaker: ["FEIL_BELOP"],
      forklaring: "Beløpet er feil. Du må justere antall deltakere",
      besluttelse: Besluttelse.AVVIST,
    },
  },
  {
    handlinger: [UtbetalingLinjeHandling.ATTESTER, UtbetalingLinjeHandling.RETURNER],
    id: "456e4567-e89b-12d3-a456-426614174002",
    tilsagn: {
      id: "3ac22799-6af6-47c7-a3f4-bb4eaa7bad07",
      type: TilsagnType.TILSAGN,
      periode: {
        start: "2025-01-01",
        slutt: "2025-03-31",
      },
      kostnadssted: {
        enhetsnummer: "0300",
        navn: "Nav Oslo",
      },
      belop: 14_000,
      belopBrukt: 4_000,
      belopGjenstaende: 10_000,
      status: {
        type: TilsagnStatus.GODKJENT,
        status: { value: "Godkjent", variant: DataElementStatusVariant.SUCCESS },
      },
      bestillingsnummer: "A-2025/123",
      kommentar: null,
    },
    status: {
      type: DelutbetalingStatus.RETURNERT,
      status: { value: "Returnert", variant: DataElementStatusVariant.ERROR },
    },
    belop: 3000,
    gjorOppTilsagn: true,
    opprettelse: {
      type: "no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto.Besluttet",
      behandletAv: {
        navn: "Bertil Bengtson",
      },
      behandletTidspunkt: "2024-01-01T22:00:00",
      besluttetAv: {
        navn: "Per Haraldsen",
      },
      besluttetTidspunkt: "2024-01-02T10:00:00",
      aarsaker: ["FEIL_BELOP"],
      forklaring: "Beløpet er feil, og bør fikses ved å endre antall deltakere",
      besluttelse: Besluttelse.AVVIST,
    },
  },
  {
    handlinger: [UtbetalingLinjeHandling.ATTESTER, UtbetalingLinjeHandling.RETURNER],
    id: "456e4567-e89b-12d3-a456-426614174002",
    tilsagn: {
      id: "3ac22799-6af6-47c7-a3f4-bb4eaa7bad07",
      type: TilsagnType.TILSAGN,
      periode: {
        start: "2025-01-01",
        slutt: "2025-06-30",
      },

      kostnadssted: {
        enhetsnummer: "0300",
        navn: "Nav Oslo",
      },
      belop: 14_000,
      belopBrukt: 4_000,
      belopGjenstaende: 10_000,
      status: {
        type: TilsagnStatus.GODKJENT,
        status: { value: "Godkjent", variant: DataElementStatusVariant.SUCCESS },
      },
      bestillingsnummer: "A-2025/123",
      kommentar: null,
    },

    status: {
      type: DelutbetalingStatus.TIL_ATTESTERING,
      status: { value: "Til godkjenning", variant: DataElementStatusVariant.WARNING },
    },
    belop: 3000,
    gjorOppTilsagn: false,
    opprettelse: {
      type: "no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto.TilBeslutning",
      behandletAv: {
        navn: "Bertil Bengtson",
      },
      behandletTidspunkt: "2025-01-01T10:00:00",
      aarsaker: [],
      forklaring: "Utbetaling for første halvår 2025",
    },
  },
  {
    handlinger: [UtbetalingLinjeHandling.ATTESTER, UtbetalingLinjeHandling.RETURNER],
    id: "456e4567-e89b-12d3-a456-426614174002",
    tilsagn: {
      id: "3ac22799-6af6-47c7-a3f4-bb4eaa7bad07",
      type: TilsagnType.TILSAGN,
      periode: {
        start: "2025-03-01",
        slutt: "2025-03-31",
      },
      kostnadssted: {
        enhetsnummer: "0300",
        navn: "Nav Oslo",
      },
      belop: 14_000,
      belopBrukt: 4_000,
      belopGjenstaende: 10_000,
      status: {
        type: TilsagnStatus.GODKJENT,
        status: { value: "Godkjent", variant: DataElementStatusVariant.SUCCESS },
      },
      bestillingsnummer: "A-2025/123",
      kommentar: null,
    },
    status: {
      type: DelutbetalingStatus.OVERFORT_TIL_UTBETALING,
      status: { value: "Overført til utbetaling", variant: DataElementStatusVariant.SUCCESS },
    },
    belop: 3000,
    gjorOppTilsagn: false,
    opprettelse: {
      type: "no.nav.mulighetsrommet.api.totrinnskontroll.api.TotrinnskontrollDto.Besluttet",
      behandletAv: {
        navn: "Bertil Bengtson",
      },
      behandletTidspunkt: "2025-01-01T10:00:00",
      aarsaker: [],
      forklaring: "Utbetaling for første halvår 2025",
      besluttetAv: {
        navn: "Per Haraldsen",
      },
      besluttetTidspunkt: "2025-01-01T10:00:00",
      besluttelse: Besluttelse.GODKJENT,
    },
  },
];

export const mockBeregning: UtbetalingBeregningDto = {
  heading: "Annen avtalt pris",
  belop: 780,
  deltakerRegioner: [],
  deltakerTableData: {
    columns: [],
    rows: [],
  },
  type: "no.nav.mulighetsrommet.api.utbetaling.api.UtbetalingBeregningDto.Fri",
};
