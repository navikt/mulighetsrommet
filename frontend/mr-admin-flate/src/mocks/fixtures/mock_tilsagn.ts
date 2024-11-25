import {
  NavEnhetStatus,
  NavEnhetType,
  TilsagnAvvisningAarsak,
  TilsagnBesluttelseStatus,
  TilsagnDto,
} from "@mr/api-client";
import { mockArrangorer } from "./mock_arrangorer";
import { mockTiltaksgjennomforinger } from "./mock_tiltaksgjennomforinger";

export const mockTilsagn: TilsagnDto[] = [
  {
    arrangor: mockArrangorer.data[0],
    beregning: { type: "FRI", belop: 14000 },
    id: "10e393b0-1b7c-4c68-9a42-b541b2f114b8",
    kostnadssted: {
      enhetsnummer: "0300",
      navn: "Nav Oslo",
      overordnetEnhet: null,
      status: NavEnhetStatus.AKTIV,
      type: NavEnhetType.TILTAK,
    },
    lopenummer: 1,
    periodeStart: "2024-01-05",
    periodeSlutt: "2024-01-06",
    tiltaksgjennomforing: {
      id: mockTiltaksgjennomforinger[0].id,
      antallPlasser: mockTiltaksgjennomforinger[0].antallPlasser || 15,
    },
    opprettetAv: "B123456",
  },
  {
    arrangor: mockArrangorer.data[0],
    beregning: { type: "FRI", belop: 14000 },
    id: "fd1825aa-1951-4de2-9b72-12d22f121e92",
    kostnadssted: {
      enhetsnummer: "0300",
      navn: "Nav Oslo",
      overordnetEnhet: null,
      status: NavEnhetStatus.AKTIV,
      type: NavEnhetType.TILTAK,
    },
    lopenummer: 1,
    periodeStart: "2024-01-03",
    periodeSlutt: "2024-01-04",
    tiltaksgjennomforing: {
      id: mockTiltaksgjennomforinger[0].id,
      antallPlasser: mockTiltaksgjennomforinger[0].antallPlasser || 15,
    },
    opprettetAv: "F123456",
  },
  {
    arrangor: mockArrangorer.data[0],
    beregning: { type: "FRI", belop: 67000 },
    id: "aaad5bed-00dc-4437-9b43-b09eced228d7",
    kostnadssted: {
      enhetsnummer: "0300",
      navn: "Nav Oslo",
      overordnetEnhet: null,
      status: NavEnhetStatus.AKTIV,
      type: NavEnhetType.TILTAK,
    },
    lopenummer: 1,
    periodeStart: "2024-01-01",
    periodeSlutt: "2024-01-02",
    tiltaksgjennomforing: {
      id: mockTiltaksgjennomforinger[0].id,
      antallPlasser: mockTiltaksgjennomforinger[0].antallPlasser || 15,
    },
    opprettetAv: "F123456",
  },
  {
    arrangor: mockArrangorer.data[0],
    beregning: { type: "FRI", belop: 67000 },
    id: "3ac22799-6af6-47c7-a3f4-bb4eaa7bad07",
    kostnadssted: {
      enhetsnummer: "0300",
      navn: "Nav Oslo",
      overordnetEnhet: null,
      status: NavEnhetStatus.AKTIV,
      type: NavEnhetType.TILTAK,
    },
    lopenummer: 4,
    periodeStart: "2024-01-01",
    periodeSlutt: "2024-01-02",
    tiltaksgjennomforing: {
      id: mockTiltaksgjennomforinger[0].id,
      antallPlasser: mockTiltaksgjennomforinger[0].antallPlasser || 15,
    },
    opprettetAv: "F123456",
    besluttelse: {
      navIdent: "N12345",
      beslutternavn: "Nils Ole Hansen",
      tidspunkt: "2024-01-10",
      status: TilsagnBesluttelseStatus.GODKJENT,
    },
  },
  {
    arrangor: mockArrangorer.data[0],
    beregning: { type: "FRI", belop: 67000 },
    id: "c7cd1ac0-34cd-46f2-b441-6d8c7318ee05",
    kostnadssted: {
      enhetsnummer: "0300",
      navn: "Nav Oslo",
      overordnetEnhet: null,
      status: NavEnhetStatus.AKTIV,
      type: NavEnhetType.TILTAK,
    },
    lopenummer: 4,
    periodeStart: "2024-01-01",
    periodeSlutt: "2024-01-02",
    tiltaksgjennomforing: {
      id: mockTiltaksgjennomforinger[0].id,
      antallPlasser: mockTiltaksgjennomforinger[0].antallPlasser || 15,
    },
    opprettetAv: "F123456",
    annullertTidspunkt: "2024-05-10",
  },
  {
    arrangor: mockArrangorer.data[0],
    beregning: { type: "FRI", belop: 67000 },
    id: "5950e714-95bc-4d4c-b52e-c75fde749056",
    kostnadssted: {
      enhetsnummer: "0300",
      navn: "Nav Oslo",
      overordnetEnhet: null,
      status: NavEnhetStatus.AKTIV,
      type: NavEnhetType.TILTAK,
    },
    lopenummer: 4,
    periodeStart: "2024-01-01",
    periodeSlutt: "2024-01-02",
    tiltaksgjennomforing: {
      id: mockTiltaksgjennomforinger[0].id,
      antallPlasser: mockTiltaksgjennomforinger[0].antallPlasser || 15,
    },
    opprettetAv: "B123456",
    besluttelse: {
      navIdent: "N12345",
      beslutternavn: "Nils Ole Hansen",
      tidspunkt: "2024-01-10",
      status: TilsagnBesluttelseStatus.AVVIST,
      aarsaker: [TilsagnAvvisningAarsak.FEIL_ANTALL_PLASSER, TilsagnAvvisningAarsak.FEIL_ANNET],
      forklaring: "Du må fikse antall plasser. Det skal være 25 plasser.",
    },
  },
];
