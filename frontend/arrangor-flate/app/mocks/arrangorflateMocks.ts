import {
  Arrangor,
  ArrangorflateTilsagn,
  ArrFlateUtbetaling,
  RelevanteForslag,
  TilsagnStatus,
  TilsagnType,
  ArrFlateUtbetalingStatus,
  ArrFlateUtbetalingKompakt,
} from "api-client";
import { http, HttpResponse, PathParams } from "msw";
import { v4 as uuid } from "uuid";

const mockDeltakelser = [
  {
    id: uuid(),
    person: {
      navn: "Per Petterson",
      fodselsdato: "1980-01-01",
      fodselsaar: 1980,
    },
    startDato: "2024-06-01",
    forstePeriodeStartDato: "2024-06-01",
    sistePeriodeSluttDato: "2024-06-30",
    sistePeriodeDeltakelsesprosent: 30,
    manedsverk: 0.3,
  },
  {
    id: uuid(),
    person: {
      navn: "Stian Bjærvik",
      fodselsaar: 1980,
    },
    startDato: "2024-06-01",
    forstePeriodeStartDato: "2024-06-01",
    sistePeriodeSluttDato: "2024-06-30",
    sistePeriodeDeltakelsesprosent: 100,
    manedsverk: 1,
  },
  {
    id: uuid(),
    person: {
      navn: "Donald Duck",
      fodselsaar: 1980,
    },
    startDato: "2024-06-01",
    forstePeriodeStartDato: "2024-06-01",
    sistePeriodeSluttDato: "2024-06-30",
    sistePeriodeDeltakelsesprosent: 100,
    manedsverk: 1,
  },
];

const mockUtbetalinger: ArrFlateUtbetalingKompakt[] = [
  {
    id: "da28997b-c2ba-4f5c-b733-94eb57e57d19",
    status: ArrFlateUtbetalingStatus.KLAR_FOR_GODKJENNING,
    fristForGodkjenning: "2024-08-01T00:00:00",
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },

    gjennomforing: {
      id: uuid(),
      navn: "AFT tiltak Moss",
    },
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
      slettet: false,
    },
    periode: {
      start: "2024-06-01",
      slutt: "2024-06-30",
    },
    belop: 308530,
  },
  {
    id: "80a49868-0d06-4243-bc39-7ac33fbada88",
    status: ArrFlateUtbetalingStatus.KLAR_FOR_GODKJENNING,
    fristForGodkjenning: "2024-08-01T00:00:00",
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    gjennomforing: {
      id: uuid(),
      navn: "Amo tiltak Halden",
    },
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
      slettet: false,
    },
    periode: {
      start: "2024-06-01",
      slutt: "2024-06-30",
    },
    belop: 85000,
  },
  {
    id: "91591ca9-ac32-484e-b95a-1a1258c5c32a",
    status: ArrFlateUtbetalingStatus.BEHANDLES_AV_NAV,
    fristForGodkjenning: "2024-08-01T00:00:00",
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    gjennomforing: {
      id: uuid(),
      navn: "Amo tiltak Halden",
    },

    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
      slettet: false,
    },
    periode: {
      start: "2024-06-01",
      slutt: "2024-06-30",
    },
    belop: 85000,
  },
  {
    id: "87b4425b-8be0-4938-94bc-2ba1ae7beb0e",
    status: ArrFlateUtbetalingStatus.UTBETALT,
    fristForGodkjenning: "2024-08-01T00:00:00",
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    gjennomforing: {
      id: uuid(),
      navn: "Amo tiltak Halden",
    },

    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
      slettet: false,
    },
    periode: {
      start: "2024-06-01",
      slutt: "2024-06-30",
    },

    belop: 85000,
  },
];

const mockKrav: ArrFlateUtbetaling[] = [
  {
    id: "da28997b-c2ba-4f5c-b733-94eb57e57d19",
    status: ArrFlateUtbetalingStatus.KLAR_FOR_GODKJENNING,
    fristForGodkjenning: "2024-08-01T00:00:00",
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    betalingsinformasjon: {
      kontonummer: "12345678901",
      kid: "123456789",
    },
    gjennomforing: {
      id: uuid(),
      navn: "AFT tiltak Moss",
    },
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
      slettet: false,
    },
    periode: {
      start: "2024-06-01",
      slutt: "2024-06-30",
    },
    beregning: {
      antallManedsverk: 17.5,
      belop: 308530,
      digest: "ac6b2cdcbfc885e64121cf4e0ebee5dd",
      deltakelser: mockDeltakelser,
      type: "FORHANDSGODKJENT",
    },
  },
  {
    id: "80a49868-0d06-4243-bc39-7ac33fbada88",
    status: ArrFlateUtbetalingStatus.KLAR_FOR_GODKJENNING,
    fristForGodkjenning: "2024-08-01T00:00:00",
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    gjennomforing: {
      id: uuid(),
      navn: "Amo tiltak Halden",
    },
    betalingsinformasjon: {
      kontonummer: "12345678901",
      kid: "123456789",
    },
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
      slettet: false,
    },
    periode: {
      start: "2024-06-01",
      slutt: "2024-06-30",
    },
    beregning: {
      antallManedsverk: 4,
      belop: 85000,
      digest: "5c25b2ae0d9b5f2c76e4a6065125cbdb",
      deltakelser: mockDeltakelser,
      type: "FORHANDSGODKJENT",
    },
  },
  {
    id: "91591ca9-ac32-484e-b95a-1a1258c5c32a",
    status: ArrFlateUtbetalingStatus.BEHANDLES_AV_NAV,
    fristForGodkjenning: "2024-08-01T00:00:00",
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    gjennomforing: {
      id: uuid(),
      navn: "Amo tiltak Halden",
    },
    betalingsinformasjon: {
      kontonummer: "12345678901",
      kid: "123456789",
    },
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
      slettet: false,
    },
    periode: {
      start: "2024-06-01",
      slutt: "2024-06-30",
    },
    beregning: {
      antallManedsverk: 4,
      belop: 85000,
      digest: "5c25b2ae0d9b5f2c76e4a6065125cbdb",
      deltakelser: mockDeltakelser,
      type: "FORHANDSGODKJENT",
    },
  },
  {
    id: "87b4425b-8be0-4938-94bc-2ba1ae7beb0e",
    status: ArrFlateUtbetalingStatus.UTBETALT,
    fristForGodkjenning: "2024-08-01T00:00:00",
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    gjennomforing: {
      id: uuid(),
      navn: "Amo tiltak Halden",
    },
    betalingsinformasjon: {
      kontonummer: "12345678901",
      kid: "123456789",
    },
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
      slettet: false,
    },
    periode: {
      start: "2024-06-01",
      slutt: "2024-06-30",
    },
    beregning: {
      deltakelser: [
        {
          id: uuid(),
          person: {
            navn: "Per Petterson",
            fodselsdato: "1980-01-01",
            fodselsaar: 1980,
          },
          startDato: "2024-06-01",
          forstePeriodeStartDato: "2024-06-01",
          sistePeriodeSluttDato: "2024-06-30",
          sistePeriodeDeltakelsesprosent: 30,
          manedsverk: 0.3,
        },
        {
          id: uuid(),
          person: {
            navn: "Stian Bjærvik",
          },
          startDato: "2024-06-01",
          forstePeriodeStartDato: "2024-06-01",
          sistePeriodeSluttDato: "2024-06-30",
          sistePeriodeDeltakelsesprosent: 100,
          manedsverk: 1,
        },
      ],
      antallManedsverk: 4,
      belop: 85000,
      digest: "5c25b2ae0d9b5f2c76e4a6065125cbdb",
      type: "FORHANDSGODKJENT",
    },
  },
];

// Mock data with all TilsagnStatus values
export const mockTilsagn: ArrangorflateTilsagn[] = [
  {
    id: uuid(),
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    periode: {
      start: "2024-07-01",
      slutt: "2024-12-31",
    },
    status: { status: TilsagnStatus.GODKJENT },
    bestillingsnummer: "A-2024/10354-2",
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
    },
    beregning: {
      type: "FORHANDSGODKJENT",
      input: {
        type: "FORHANDSGODKJENT",
        periode: {
          start: "2024-07-01",
          slutt: "2024-12-31",
        },
        antallPlasser: 15,
        sats: 20205,
      },
      output: {
        type: "FORHANDSGODKJENT",
        belop: 150000,
      },
    },
    gjenstaendeBelop: 100000,
    gjennomforing: {
      id: uuid(),
      navn: "AFT tiltak Halden",
    },
    type: TilsagnType.TILSAGN,
  },

  {
    id: uuid(),
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    periode: {
      start: "2024-09-01",
      slutt: "2024-12-31",
    },
    status: { status: TilsagnStatus.TIL_ANNULLERING },
    bestillingsnummer: "A-2024/10354-4",
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
    },
    beregning: {
      type: "FORHANDSGODKJENT",
      input: {
        type: "FORHANDSGODKJENT",
        periode: {
          start: "2024-09-01",
          slutt: "2024-12-31",
        },
        antallPlasser: 5,
        sats: 20205,
      },
      output: {
        type: "FORHANDSGODKJENT",
        belop: 50000,
      },
    },
    gjenstaendeBelop: 50000,
    gjennomforing: {
      id: uuid(),
      navn: "AFT tiltak Sarpsborg",
    },
    type: TilsagnType.TILSAGN,
  },
  {
    id: uuid(),
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    periode: {
      start: "2024-10-01",
      slutt: "2024-12-31",
    },
    status: { status: TilsagnStatus.ANNULLERT },
    bestillingsnummer: "A-2024/10354-5",
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
    },
    beregning: {
      type: "FORHANDSGODKJENT",
      input: {
        type: "FORHANDSGODKJENT",
        periode: {
          start: "2024-10-01",
          slutt: "2024-12-31",
        },
        antallPlasser: 3,
        sats: 20205,
      },
      output: {
        type: "FORHANDSGODKJENT",
        belop: 30000,
      },
    },
    gjenstaendeBelop: 0,
    gjennomforing: {
      id: uuid(),
      navn: "AFT tiltak Råde",
    },
    type: TilsagnType.TILSAGN,
  },
  {
    id: uuid(),
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    periode: {
      start: "2024-11-01",
      slutt: "2024-12-31",
    },
    status: { status: TilsagnStatus.TIL_OPPGJOR },
    bestillingsnummer: "A-2024/10354-6",
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
    },
    beregning: {
      type: "FORHANDSGODKJENT",
      input: {
        type: "FORHANDSGODKJENT",
        periode: {
          start: "2024-11-01",
          slutt: "2024-12-31",
        },
        antallPlasser: 2,
        sats: 20205,
      },
      output: {
        type: "FORHANDSGODKJENT",
        belop: 20000,
      },
    },
    gjenstaendeBelop: 20000,
    gjennomforing: {
      id: uuid(),
      navn: "AFT tiltak Våler",
    },
    type: TilsagnType.TILSAGN,
  },
  {
    id: uuid(),
    tiltakstype: {
      navn: "Arbeidsforberedende trening",
    },
    periode: {
      start: "2024-12-01",
      slutt: "2024-12-31",
    },
    status: { status: TilsagnStatus.OPPGJORT },
    bestillingsnummer: "A-2024/10354-7",
    arrangor: {
      id: uuid(),
      organisasjonsnummer: "123456789",
      navn: "Fretex",
    },
    beregning: {
      type: "FORHANDSGODKJENT",
      input: {
        type: "FORHANDSGODKJENT",
        periode: {
          start: "2024-12-01",
          slutt: "2024-12-31",
        },
        antallPlasser: 1,
        sats: 20205,
      },
      output: {
        type: "FORHANDSGODKJENT",
        belop: 10000,
      },
    },
    gjenstaendeBelop: 0,
    gjennomforing: {
      id: uuid(),
      navn: "AFT tiltak Indre Østfold",
    },
    type: TilsagnType.TILSAGN,
  },
];

const arrangorer: Arrangor[] = [
  {
    id: uuid(),
    organisasjonsnummer: "123456789",
    organisasjonsform: "AS",
    navn: "Fretex",
    overordnetEnhet: null,
  },
];

const mockRelevanteForslag: RelevanteForslag[] = [
  {
    deltakerId: mockDeltakelser[0].id,
    antallRelevanteForslag: 1,
  },
  {
    deltakerId: mockDeltakelser[1].id,
    antallRelevanteForslag: 0,
  },
];

export const arrangorflateHandlers = [
  http.get<PathParams, ArrFlateUtbetaling[]>(
    "*/api/v1/intern/arrangorflate/arrangor/:orgnr/utbetaling",
    () => HttpResponse.json(mockUtbetalinger),
  ),
  http.get<PathParams, ArrFlateUtbetaling[]>(
    "*/api/v1/intern/arrangorflate/utbetaling/:id",
    ({ params }) => {
      const { id } = params;
      return HttpResponse.json(mockKrav.find((k) => k.id === id));
    },
  ),
  http.post<PathParams, ArrFlateUtbetaling[]>(
    "*/api/v1/intern/arrangorflate/utbetaling/:id/godkjenn",
    () => HttpResponse.json({}),
  ),
  http.get<PathParams, ArrFlateUtbetaling[]>(
    "*/api/v1/intern/arrangorflate/:orgnr/utbetaling/:id/kvittering",
    () => HttpResponse.json(undefined, { status: 501 }),
  ),
  http.get<PathParams, ArrFlateUtbetaling[]>(
    "*/api/v1/intern/arrangorflate/utbetaling/:id/tilsagn",
    () => HttpResponse.json(mockTilsagn),
  ),
  http.get<PathParams, ArrFlateUtbetaling[]>(
    "*/api/v1/intern/arrangorflate/utbetaling/:id/relevante-forslag",
    () => HttpResponse.json(mockRelevanteForslag),
  ),
  http.get<PathParams, ArrFlateUtbetaling[]>(
    "*/api/v1/intern/arrangorflate/arrangor/:orgnr/tilsagn",
    () => HttpResponse.json(mockTilsagn),
  ),
  http.get<PathParams, ArrFlateUtbetaling[]>(
    "*/api/v1/intern/arrangorflate/tilsagn/:id",
    ({ params }) => {
      const { id } = params;
      return HttpResponse.json(
        mockTilsagn.find((k) => k.id === id) || mockTilsagn.find((k) => k.id === id),
      );
    },
  ),
  http.get<PathParams, Arrangor[]>("*/api/v1/intern/arrangorflate/tilgang-arrangor", () =>
    HttpResponse.json(arrangorer),
  ),
];
