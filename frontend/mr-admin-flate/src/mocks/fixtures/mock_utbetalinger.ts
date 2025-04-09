import {
  AdminUtbetalingStatus,
  Besluttelse,
  DelutbetalingStatus,
  NavEnhetStatus,
  NavEnhetType,
  TilsagnStatus,
  TilsagnType,
  TotrinnskontrollBesluttetDto,
  TotrinnskontrollTilBeslutningDto,
  UtbetalingDto,
  UtbetalingLinje,
} from "@mr/api-client-v2";

export const mockUtbetalinger: UtbetalingDto[] = [
  {
    id: "123e4567-e89b-12d3-a456-426614174000",
    periode: {
      start: "2024-01-01",
      slutt: "2024-06-30",
    },
    status: AdminUtbetalingStatus.VENTER_PA_ARRANGOR,
    createdAt: "2024-01-01T10:00:00",
    godkjentAvArrangorTidspunkt: undefined,
    beregning: {
      belop: 15000,
    },
    betalingsinformasjon: {
      kontonummer: "1234.56.78900",
      kid: "12345678901",
    },
    beskrivelse: "Utbetaling for første halvår 2024",
    innsendtAv: "Z123456",
    journalpostId: "JP123456",
  },
  {
    id: "123e4567-e89b-12d3-a456-426614174001",
    periode: {
      start: "2025-01-01",
      slutt: "2025-06-30",
    },
    status: AdminUtbetalingStatus.TIL_GODKJENNING,
    createdAt: "2024-07-01T14:30:00",
    godkjentAvArrangorTidspunkt: "2024-07-02T09:15:00",
    beregning: {
      belop: 18000,
    },
    betalingsinformasjon: {
      kontonummer: "9876.54.32100",
      kid: "98765432109",
    },
    beskrivelse: "Utbetaling for andre halvår 2024",
    innsendtAv: "Arrangør",
    journalpostId: "JP123457",
  },
  {
    id: "123e4567-e89b-12d3-a456-426614174002",
    periode: {
      start: "2025-01-01",
      slutt: "2025-03-31",
    },
    status: AdminUtbetalingStatus.RETURNERT,
    createdAt: "2025-01-01T08:00:00",
    godkjentAvArrangorTidspunkt: undefined,
    beregning: {
      belop: 9000,
    },
    betalingsinformasjon: {
      kontonummer: "1111.22.33333",
      kid: "11122233344",
    },
    beskrivelse: "Utbetaling for første kvartal 2025",
    innsendtAv: "Z987654",
    journalpostId: "JP123458",
  },
  {
    id: "129e4567-e89b-12d3-a456-426614174002",
    periode: {
      start: "2025-03-01",
      slutt: "2025-03-31",
    },
    status: AdminUtbetalingStatus.OVERFORT_TIL_UTBETALING,
    createdAt: "2025-01-01T08:00:00",
    godkjentAvArrangorTidspunkt: undefined,
    beregning: {
      belop: 9000,
    },
    betalingsinformasjon: {
      kontonummer: "1111.22.33333",
      kid: "11122233344",
    },
    beskrivelse: "Utbetaling for første kvartal 2025",
    innsendtAv: "Z987654",
    journalpostId: "JP123458",
  },
  {
    id: "130e4567-e89b-12d3-a456-426614174002",
    periode: {
      start: "2025-06-01",
      slutt: "2025-06-31",
    },
    status: AdminUtbetalingStatus.UTBETALT,
    createdAt: "2025-01-01T08:00:00",
    godkjentAvArrangorTidspunkt: undefined,
    beregning: {
      belop: 9000,
    },
    betalingsinformasjon: {
      kontonummer: "1111.22.33333",
      kid: "11122233344",
    },
    beskrivelse: "Utbetaling for første kvartal 2025",
    innsendtAv: "Z987654",
    journalpostId: "JP123458",
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
      belopGjenstaende: 10000,
      kostnadssted: {
        enhetsnummer: "0300",
        navn: "Nav Oslo",
        overordnetEnhet: null,
        status: NavEnhetStatus.AKTIV,
        type: NavEnhetType.TILTAK,
      },
      beregning: {
        type: "FRI",
        input: { type: "FRI", belop: 14000 },
        output: { type: "FRI", belop: 14000 },
      },
      status: TilsagnStatus.TIL_GODKJENNING,
      bestillingsnummer: "A-2024/123",
    },
    status: DelutbetalingStatus.TIL_GODKJENNING,
    belop: 5000,
    gjorOppTilsagn: true,
    opprettelse: {
      type: "TIL_BESLUTNING",
      behandletAv: {
        type: "B123456",
        navn: "Bertil Bengtson",
      },
      behandletTidspunkt: "2024-01-01T22:00:00",
      aarsaker: ["Utbetaling for første halvår 2024"],
      forklaring: "Utbetaling for tilsagn",
      kanBesluttes: true,
    } as TotrinnskontrollTilBeslutningDto,
  },
  {
    id: "456e4567-e89b-12d3-a456-426614174001",
    tilsagn: {
      id: "fd1825aa-1951-4de2-9b72-12d22f121e92",
      type: TilsagnType.TILSAGN,
      periode: {
        start: "2024-07-01",
        slutt: "2024-12-31",
      },
      belopGjenstaende: 10000,
      kostnadssted: {
        enhetsnummer: "0300",
        navn: "Nav Oslo",
        overordnetEnhet: null,
        status: NavEnhetStatus.AKTIV,
        type: NavEnhetType.TILTAK,
      },
      beregning: {
        type: "FRI",
        input: { type: "FRI", belop: 14000 },
        output: { type: "FRI", belop: 14000 },
      },
      status: TilsagnStatus.TIL_ANNULLERING,
      bestillingsnummer: "A-2024/123",
    },
    status: DelutbetalingStatus.RETURNERT,
    belop: 7500,
    gjorOppTilsagn: false,
    opprettelse: {
      type: "BESLUTTET",
      behandletAv: {
        type: "B123456",
        navn: "Bertil Bengtson",
      },
      behandletTidspunkt: "2024-01-01T22:00:00",
      besluttetAv: {
        type: "P654321",
        navn: "Per Haraldsen",
      },
      besluttetTidspunkt: "2024-01-02T10:00:00",
      aarsaker: ["FEIL_BELOP"],
      forklaring: "Beløpet er feil. Du må justere antall deltakere",
      kanBesluttes: false,
      besluttelse: Besluttelse.AVVIST,
    } as TotrinnskontrollBesluttetDto,
  },
  {
    id: "456e4567-e89b-12d3-a456-426614174002",
    tilsagn: {
      id: "3ac22799-6af6-47c7-a3f4-bb4eaa7bad07",
      type: TilsagnType.TILSAGN,
      periode: {
        start: "2025-01-01",
        slutt: "2025-03-31",
      },
      belopGjenstaende: 10000,
      kostnadssted: {
        enhetsnummer: "0300",
        navn: "Nav Oslo",
        overordnetEnhet: null,
        status: NavEnhetStatus.AKTIV,
        type: NavEnhetType.TILTAK,
      },
      beregning: {
        type: "FRI",
        input: { type: "FRI", belop: 14000 },
        output: { type: "FRI", belop: 14000 },
      },
      status: TilsagnStatus.GODKJENT,
      bestillingsnummer: "A-2025/123",
    },
    status: DelutbetalingStatus.RETURNERT,
    belop: 3000,
    gjorOppTilsagn: true,
    opprettelse: {
      type: "BESLUTTET",
      behandletAv: {
        type: "B123456",
        navn: "Bertil Bengtson",
      },
      behandletTidspunkt: "2024-01-01T22:00:00",
      besluttetAv: {
        type: "P654321",
        navn: "Per Haraldsen",
      },
      besluttetTidspunkt: "2024-01-02T10:00:00",
      aarsaker: ["FEIL_BELOP"],
      forklaring: "Beløpet er feil, og bør fikses ved å endre antall deltakere",
      kanBesluttes: false,
      besluttelse: Besluttelse.AVVIST,
    } as TotrinnskontrollBesluttetDto,
  },
  {
    id: "456e4567-e89b-12d3-a456-426614174002",
    tilsagn: {
      id: "3ac22799-6af6-47c7-a3f4-bb4eaa7bad07",
      type: TilsagnType.TILSAGN,
      periode: {
        start: "2025-01-01",
        slutt: "2025-06-30",
      },
      belopGjenstaende: 10000,
      kostnadssted: {
        enhetsnummer: "0300",
        navn: "Nav Oslo",
        overordnetEnhet: null,
        status: NavEnhetStatus.AKTIV,
        type: NavEnhetType.TILTAK,
      },
      beregning: {
        type: "FRI",
        input: { type: "FRI", belop: 14000 },
        output: { type: "FRI", belop: 14000 },
      },
      status: TilsagnStatus.GODKJENT,
      bestillingsnummer: "A-2025/123",
    },

    status: DelutbetalingStatus.TIL_GODKJENNING,
    belop: 3000,
    gjorOppTilsagn: false,
    opprettelse: {
      type: "TIL_BESLUTNING",
      behandletAv: {
        type: "B123456",
        navn: "Bertil Bengtson",
      },
      behandletTidspunkt: "2025-01-01T10:00:00",
      aarsaker: [],
      forklaring: "Utbetaling for første halvår 2025",
      kanBesluttes: true,
    } as TotrinnskontrollTilBeslutningDto,
  },
  {
    id: "456e4567-e89b-12d3-a456-426614174002",
    tilsagn: {
      id: "3ac22799-6af6-47c7-a3f4-bb4eaa7bad07",
      type: TilsagnType.TILSAGN,
      periode: {
        start: "2025-03-01",
        slutt: "2025-03-31",
      },
      belopGjenstaende: 10000,
      kostnadssted: {
        enhetsnummer: "0300",
        navn: "Nav Oslo",
        overordnetEnhet: null,
        status: NavEnhetStatus.AKTIV,
        type: NavEnhetType.TILTAK,
      },
      beregning: {
        type: "FRI",
        input: { type: "FRI", belop: 14000 },
        output: { type: "FRI", belop: 14000 },
      },
      status: TilsagnStatus.GODKJENT,
      bestillingsnummer: "A-2025/123",
    },

    status: DelutbetalingStatus.OVERFORT_TIL_UTBETALING,
    belop: 3000,
    gjorOppTilsagn: false,
    opprettelse: {
      type: "BESLUTTET",
      behandletAv: {
        type: "B123456",
        navn: "Bertil Bengtson",
      },
      behandletTidspunkt: "2025-01-01T10:00:00",
      aarsaker: [],
      forklaring: "Utbetaling for første halvår 2025",
      kanBesluttes: true,
      besluttetAv: {
        type: "P654321",
        navn: "Per Haraldsen",
      },
      besluttetTidspunkt: "2025-01-01T10:00:00",
      besluttelse: Besluttelse.GODKJENT,
    } as TotrinnskontrollBesluttetDto,
  },
  {
    id: "456e4567-e89b-12d3-a456-426614174002",
    tilsagn: {
      id: "3ac22799-6af6-47c7-a3f4-bb4eaa7bad07",
      type: TilsagnType.TILSAGN,
      periode: {
        start: "2025-06-01",
        slutt: "2025-06-31",
      },
      belopGjenstaende: 10000,
      kostnadssted: {
        enhetsnummer: "0300",
        navn: "Nav Oslo",
        overordnetEnhet: null,
        status: NavEnhetStatus.AKTIV,
        type: NavEnhetType.TILTAK,
      },
      beregning: {
        type: "FRI",
        input: { type: "FRI", belop: 14000 },
        output: { type: "FRI", belop: 14000 },
      },
      status: TilsagnStatus.GODKJENT,
      bestillingsnummer: "A-2025/123",
    },

    status: DelutbetalingStatus.UTBETALT,
    belop: 3000,
    gjorOppTilsagn: false,
    opprettelse: {
      type: "BESLUTTET",
      behandletAv: {
        type: "B123456",
        navn: "Bertil Bengtson",
      },
      behandletTidspunkt: "2025-01-01T10:00:00",
      aarsaker: [],
      forklaring: "Utbetaling for første halvår 2025",
      kanBesluttes: true,
      besluttetAv: {
        type: "P654321",
        navn: "Per Haraldsen",
      },
      besluttetTidspunkt: "2025-01-01T10:00:00",
      besluttelse: Besluttelse.GODKJENT,
    } as TotrinnskontrollBesluttetDto,
  },
];
