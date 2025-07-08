import { http, HttpResponse, PathParams } from "msw";
import { mockTilsagn } from "./arrangorflateMocks";
import {
  Arrangor,
  ArrFlateUtbetaling,
  RelevanteForslag,
  UtbetalingDeltakelseManedsverk,
} from "api-client";
import { mockArrFlateUtbetalingKompakt } from "./oversiktMocks";
import { arrFlateUtbetaling } from "./utbetalingMocks";
import { v4 as uuid } from "uuid";

const mockDeltakelser: UtbetalingDeltakelseManedsverk[] = [
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
    perioder: [
      {
        periode: { start: "2024-06-01", slutt: "2024-06-15" },
        deltakelsesprosent: 100,
      },
      {
        periode: { start: "2024-06-15", slutt: "2024-07-01" },
        deltakelsesprosent: 30,
      },
    ],
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
    perioder: [
      {
        periode: { start: "2024-06-01", slutt: "2024-07-01" },
        deltakelsesprosent: 100,
      },
    ],
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
    perioder: [
      {
        periode: { start: "2024-06-01", slutt: "2024-07-01" },
        deltakelsesprosent: 100,
      },
    ],
    manedsverk: 1,
  },
];

const arrangorMock: Arrangor = {
  id: uuid(),
  organisasjonsnummer: "123456789",
  organisasjonsform: "AS",
  navn: "Arrangør",
  overordnetEnhet: null,
};

const mockRelevanteForslag: RelevanteForslag[] = [
  {
    deltakerId: mockDeltakelser[0].id,
    antallRelevanteForslag: 0,
  },
  {
    deltakerId: mockDeltakelser[1].id,
    antallRelevanteForslag: 0,
  },
];

export const handlers = [
  http.get<PathParams, ArrFlateUtbetaling[]>(
    "*/api/v1/intern/arrangorflate/arrangor/:orgnr/utbetaling",
    () => HttpResponse.json(mockArrFlateUtbetalingKompakt),
  ),
  http.get<PathParams, ArrFlateUtbetaling[]>(
    "*/api/v1/intern/arrangorflate/utbetaling/:id",
    ({ params }) => {
      const { id } = params;
      return HttpResponse.json(arrFlateUtbetaling.find((k) => k.id === id));
    },
  ),
  http.get<PathParams, string>(
    "*/api/v1/intern/arrangorflate/utbetaling/:id/sync-kontonummer",
    ({ params }) => {
      const { id } = params;
      const kontoNr = arrFlateUtbetaling.find((k) => k.id === id)?.betalingsinformasjon.kontonummer;
      const expires = new Date(new Date().getTime() + 5 * 60000).toISOString(); // 5 min, NS_BINDING_ABORTED fix
      return HttpResponse.text(kontoNr, {
        headers: { Expires: expires },
      });
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
  http.get<PathParams, boolean>(
    "*/api/v1/intern/arrangorflate/arrangor/:orgnr/features",
    () => new HttpResponse(true, { status: 200 }),
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
    HttpResponse.json([arrangorMock]),
  ),
];
