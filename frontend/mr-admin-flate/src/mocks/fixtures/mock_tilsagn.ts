import {
  NavEnhetStatus,
  NavEnhetType,
  TilsagnAvvisningAarsak,
  TilsagnDto,
  TilsagnStatus,
  TilsagnTilAnnulleringAarsak,
  TilsagnType,
} from "@mr/api-client-v2";
import { mockArrangorer } from "./mock_arrangorer";

export const mockTilsagn: TilsagnDto[] = [
  {
    arrangor: mockArrangorer.data[0],
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
    lopenummer: 1,
    periode: {
      start: "2024-01-05",
      slutt: "2024-01-06",
    },
    status: TilsagnStatus.TIL_GODKJENNING,
    opprettelse: {
      behandletAv: "B123456",
      behandletTidspunkt: "2024-01-01T22:00:00",
    },
  },
  {
    arrangor: mockArrangorer.data[0],
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
    lopenummer: 1,
    periode: {
      start: "2024-01-03",
      slutt: "2024-01-04",
    },
    status: TilsagnStatus.TIL_ANNULLERING,
    opprettelse: {
      behandletAv: "B123456",
      behandletTidspunkt: "2024-01-01T22:00:00",
      besluttetAv: "F123456",
      besluttetTidspunkt: "2024-01-01T22:00:00",
    },
    annullering: {
      behandletAv: "B123456",
      behandletTidspunkt: "2024-01-01T22:00:00",
      aarsaker: [
        TilsagnTilAnnulleringAarsak.FEIL_REGISTRERING,
        TilsagnTilAnnulleringAarsak.FEIL_ANNET,
      ],
      forklaring: "Du må fikse det",
    },
  },
  {
    arrangor: mockArrangorer.data[0],
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
    lopenummer: 4,
    periode: {
      start: "2024-01-01",
      slutt: "2024-01-02",
    },
    status: TilsagnStatus.GODKJENT,
    opprettelse: {
      behandletAv: "B123456",
      behandletTidspunkt: "2024-01-01T22:00:00",
      besluttetAv: "F123456",
      besluttetTidspunkt: "2024-01-01T22:00:00",
    },
  },
  {
    arrangor: mockArrangorer.data[0],
    type: TilsagnType.TILSAGN,
    beregning: {
      type: "FRI",
      input: { type: "FRI", belop: 14000 },
      output: { type: "FRI", belop: 14000 },
    },
    id: "c7cd1ac0-34cd-46f2-b441-6d8c7318ee05",
    kostnadssted: {
      enhetsnummer: "0300",
      navn: "Nav Oslo",
      overordnetEnhet: null,
      status: NavEnhetStatus.AKTIV,
      type: NavEnhetType.TILTAK,
    },
    lopenummer: 4,
    periode: {
      start: "2024-01-01",
      slutt: "2024-01-02",
    },
    status: TilsagnStatus.ANNULLERT,
    opprettelse: {
      behandletAv: "B123456",
      behandletTidspunkt: "2024-01-01T22:00:00",
      besluttetAv: "F123456",
      besluttetTidspunkt: "2024-01-01T22:00:00",
    },
    annullering: {
      behandletAv: "B123456",
      behandletTidspunkt: "2024-01-01T22:00:00",
      aarsaker: [
        TilsagnTilAnnulleringAarsak.FEIL_REGISTRERING,
        TilsagnTilAnnulleringAarsak.FEIL_ANNET,
      ],
      forklaring: "Du må fikse antall plasser. Det skal være 25 plasser.",
      besluttetAv: "F123456",
      besluttetTidspunkt: "2024-01-01T22:00:00",
    },
  },
  {
    arrangor: mockArrangorer.data[0],
    type: TilsagnType.TILSAGN,
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
    lopenummer: 4,
    periode: {
      start: "2024-01-01",
      slutt: "2024-01-02",
    },
    status: TilsagnStatus.RETURNERT,
    opprettelse: {
      behandletAv: "B123456",
      behandletTidspunkt: "2024-01-09",
      besluttetAv: "N12345",
      besluttetTidspunkt: "2024-01-10",
      aarsaker: [TilsagnAvvisningAarsak.FEIL_ANTALL_PLASSER, TilsagnAvvisningAarsak.FEIL_ANNET],
      forklaring: "Du må fikse antall plasser. Det skal være 25 plasser.",
    },
  },
];
