import {
  NavEnhetStatus,
  NavEnhetType,
  TilsagnDto,
  TilsagnStatus,
  TilsagnType,
} from "@mr/api-client-v2";

export const mockTilsagn: TilsagnDto[] = [
  {
    belopBrukt: 4000,
    type: TilsagnType.TILSAGN,
    beregning: {
      type: "FRI",
      input: { type: "FRI", belop: 14000 },
      output: { type: "FRI", belop: 14000 },
    },
    id: "10e393b0-1b7c-4c68-9a42-b541b2f114b8",
    kostnadssted: {
      enhetsnummer: "0300",
      navn: "Nav Oslo",
      overordnetEnhet: null,
      status: NavEnhetStatus.AKTIV,
      type: NavEnhetType.TILTAK,
    },
    periode: {
      start: "2024-01-05",
      slutt: "2024-01-06",
    },
    status: TilsagnStatus.TIL_GODKJENNING,
    bestillingsnummer: "A-2024/123",
  },
  {
    belopBrukt: 4000,
    type: TilsagnType.TILSAGN,
    beregning: {
      type: "FRI",
      input: { type: "FRI", belop: 14000 },
      output: { type: "FRI", belop: 14000 },
    },
    id: "fd1825aa-1951-4de2-9b72-12d22f121e92",
    kostnadssted: {
      enhetsnummer: "0300",
      navn: "Nav Oslo",
      overordnetEnhet: null,
      status: NavEnhetStatus.AKTIV,
      type: NavEnhetType.TILTAK,
    },
    periode: {
      start: "2024-01-03",
      slutt: "2024-01-04",
    },
    status: TilsagnStatus.TIL_ANNULLERING,
    bestillingsnummer: "A-2024/123",
  },
  {
    belopBrukt: 4000,
    type: TilsagnType.TILSAGN,
    beregning: {
      type: "FRI",
      input: { type: "FRI", belop: 14000 },
      output: { type: "FRI", belop: 14000 },
    },
    id: "3ac22799-6af6-47c7-a3f4-bb4eaa7bad07",
    kostnadssted: {
      enhetsnummer: "0300",
      navn: "Nav Oslo",
      overordnetEnhet: null,
      status: NavEnhetStatus.AKTIV,
      type: NavEnhetType.TILTAK,
    },
    periode: {
      start: "2024-01-01",
      slutt: "2024-01-02",
    },
    status: TilsagnStatus.GODKJENT,
    bestillingsnummer: "A-2024/123",
  },
  {
    type: TilsagnType.TILSAGN,
    beregning: {
      type: "FRI",
      input: { type: "FRI", belop: 14000 },
      output: { type: "FRI", belop: 14000 },
    },
    id: "c7cd1ac0-34cd-46f2-b441-6d8c7318ee05",
    belopBrukt: 4000,
    kostnadssted: {
      enhetsnummer: "0300",
      navn: "Nav Oslo",
      overordnetEnhet: null,
      status: NavEnhetStatus.AKTIV,
      type: NavEnhetType.TILTAK,
    },
    periode: {
      start: "2024-01-01",
      slutt: "2024-01-02",
    },
    status: TilsagnStatus.ANNULLERT,
    bestillingsnummer: "A-2024/123",
  },
  {
    type: TilsagnType.TILSAGN,
    belopBrukt: 4000,
    beregning: {
      type: "FRI",
      input: { type: "FRI", belop: 14000 },
      output: { type: "FRI", belop: 14000 },
    },
    id: "5950e714-95bc-4d4c-b52e-c75fde749056",
    kostnadssted: {
      enhetsnummer: "0300",
      navn: "Nav Oslo",
      overordnetEnhet: null,
      status: NavEnhetStatus.AKTIV,
      type: NavEnhetType.TILTAK,
    },
    periode: {
      start: "2024-01-01",
      slutt: "2024-01-02",
    },
    status: TilsagnStatus.RETURNERT,
    bestillingsnummer: "A-2024/123",
  },
];
