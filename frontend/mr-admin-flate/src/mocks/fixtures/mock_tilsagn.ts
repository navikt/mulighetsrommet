import { NavEnhetStatus, NavEnhetType, TilsagnDto } from "@mr/api-client";
import { mockArrangorer } from "./mock_arrangorer";
import { mockTiltaksgjennomforinger } from "./mock_tiltaksgjennomforinger";

export const mockTilsagn: TilsagnDto[] = [
  {
    arrangor: mockArrangorer.data[0],
    belop: 14000,
    id: "10e393b0-1b7c-4c68-9a42-b541b2f114b8",
    kostnadssted: {
      enhetsnummer: "0300",
      navn: "NAV Oslo",
      overordnetEnhet: null,
      status: NavEnhetStatus.AKTIV,
      type: NavEnhetType.TILTAK,
    },
    lopenummer: 1,
    periodeStart: "2024-01-05",
    periodeSlutt: "2024-01-06",
    tiltaksgjennomforingId: mockTiltaksgjennomforinger[0].id,
    opprettetAv: "B123456",
  },
  {
    arrangor: mockArrangorer.data[0],
    belop: 14000,
    id: "fd1825aa-1951-4de2-9b72-12d22f121e92",
    kostnadssted: {
      enhetsnummer: "0300",
      navn: "NAV Oslo",
      overordnetEnhet: null,
      status: NavEnhetStatus.AKTIV,
      type: NavEnhetType.TILTAK,
    },
    lopenummer: 1,
    periodeStart: "2024-01-03",
    periodeSlutt: "2024-01-04",
    tiltaksgjennomforingId: mockTiltaksgjennomforinger[0].id,
    opprettetAv: "F123456",
  },
  {
    arrangor: mockArrangorer.data[0],
    belop: 67000,
    id: "5950e714-95bc-4d4c-b52e-c75fde749056",
    kostnadssted: {
      enhetsnummer: "0300",
      navn: "NAV Oslo",
      overordnetEnhet: null,
      status: NavEnhetStatus.AKTIV,
      type: NavEnhetType.TILTAK,
    },
    lopenummer: 1,
    periodeStart: "2024-01-01",
    periodeSlutt: "2024-01-02",
    tiltaksgjennomforingId: mockTiltaksgjennomforinger[0].id,
    opprettetAv: "F123456",
  },
];
