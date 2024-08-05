import { NavEnhetStatus, NavEnhetType, TilsagnDto } from "mulighetsrommet-api-client";
import { mockArrangorer } from "./mock_arrangorer";
import { mockTiltaksgjennomforinger } from "./mock_tiltaksgjennomforinger";

export const mockTilsagn: TilsagnDto[] = [
  {
    arrangor: mockArrangorer.data[0],
    belop: 15000,
    id: window.crypto.randomUUID(),
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
  },
  {
    arrangor: mockArrangorer.data[0],
    belop: 180000,
    id: window.crypto.randomUUID(),
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
  },
  {
    arrangor: mockArrangorer.data[0],
    belop: 67000,
    id: window.crypto.randomUUID(),
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
  },
];
