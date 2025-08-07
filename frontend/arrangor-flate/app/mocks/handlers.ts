import { http, HttpResponse, PathParams } from "msw";
import {
  Arrangor,
  ArrFlateUtbetaling,
  ArrFlateUtbetalingKompakt,
  ArrFlateUtbetalingStatus,
} from "api-client";
import { mockArrFlateUtbetalingKompakt } from "./utbetalingOversiktMocks";
import { arrFlateUtbetaling } from "./utbetalingDetaljerMocks";
import { arrangorflateTilsagn } from "./tilsagnMocks";

const arrangorMock: Arrangor = {
  id: "cc04c391-d733-4762-8208-b0dd4387a126",
  organisasjonsnummer: "123456789",
  organisasjonsform: "AS",
  navn: "Arrangør",
  overordnetEnhet: null,
};

function isAktiv(utbetaling: ArrFlateUtbetalingKompakt): boolean {
  switch (utbetaling.status) {
    case ArrFlateUtbetalingStatus.KLAR_FOR_GODKJENNING:
    case ArrFlateUtbetalingStatus.BEHANDLES_AV_NAV:
    case ArrFlateUtbetalingStatus.VENTER_PA_ENDRING:
      return true;
    case ArrFlateUtbetalingStatus.OVERFORT_TIL_UTBETALING:
    case ArrFlateUtbetalingStatus.UTBETALT:
    case ArrFlateUtbetalingStatus.AVBRUTT:
      return false;
  }
}

export const handlers = [
  http.get<PathParams, ArrFlateUtbetaling[]>(
    "*/api/v1/intern/arrangorflate/arrangor/:orgnr/utbetaling",
    () =>
      HttpResponse.json({
        aktive: mockArrFlateUtbetalingKompakt.filter((u) => isAktiv(u)),
        historiske: mockArrFlateUtbetalingKompakt.filter((u) => !isAktiv(u)),
      }),
  ),
  http.get<PathParams, ArrFlateUtbetaling[]>(
    "*/api/v1/intern/arrangorflate/arrangor/:orgnr/kontonummer",
    () => HttpResponse.text("10002427740"), // random kontonr
  ),
  http.post<PathParams, ArrFlateUtbetaling[]>(
    "*/api/v1/intern/arrangorflate/arrangor/:orgnr/utbetaling",
    () => HttpResponse.text("585a2834-338a-4ac7-82e0-e1b08bfe1408"), // investerings utbetaling
  ),
  http.get<PathParams, ArrFlateUtbetaling[]>(
    "*/api/v1/intern/arrangorflate/utbetaling/:id",
    ({ params }) => {
      const { id } = params;
      const utbetaling = arrFlateUtbetaling.find((k) => k.id === id);
      if (utbetaling?.id === "fdbb7433-b42e-4cd6-b995-74a8e487329f") {
        return HttpResponse.json({
          ...utbetaling,
          godkjentAvArrangorTidspunkt: "2025-05-15T11:03:21.959059",
        });
      }
      return HttpResponse.json(utbetaling);
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
    ({ params }) => {
      const { id } = params;
      const utbetaling = arrFlateUtbetaling.find((u) => u.id === id);
      return HttpResponse.json(
        arrangorflateTilsagn.filter((it) => it.gjennomforing.id === utbetaling?.gjennomforing.id),
      );
    },
  ),
  http.get<PathParams, ArrFlateUtbetaling[]>(
    "*/api/v1/intern/arrangorflate/utbetaling/:id/relevante-forslag",
    ({ params }) => {
      if (params.id !== "a5499e34-9fb4-49d1-a37d-11810f6df19b") {
        return HttpResponse.json([]);
      }
      const utbetaling = arrFlateUtbetaling.find((it) => it.id === params.id);
      const deltakelser =
        utbetaling!.beregning.type !== "FRI" ? utbetaling!.beregning.deltakelser : [];
      const deltaker = deltakelser[Math.floor(Math.random() * deltakelser.length)];
      return HttpResponse.json([
        {
          deltakerId: deltaker.id,
          antallRelevanteForslag: 1,
        },
      ]);
    },
  ),
  http.get<PathParams, ArrFlateUtbetaling[]>(
    "*/api/v1/intern/arrangorflate/utbetaling/:id/utbetalingsdetaljer",
    () =>
      HttpResponse.json(
        { type: "method-not-allowed", title: "PDF er ikke tilgjengelig i Demo", status: "405" },
        { status: 405 },
      ),
  ),
  http.get<PathParams, ArrFlateUtbetaling[]>(
    "*/api/v1/intern/arrangorflate/arrangor/:orgnr/tilsagn",
    () => HttpResponse.json(arrangorflateTilsagn),
  ),
  http.get<PathParams, boolean>(
    "*/api/v1/intern/arrangorflate/arrangor/:orgnr/features",
    () => new HttpResponse(true, { status: 200 }),
  ),
  http.get<PathParams, boolean>(
    "*/api/v1/intern/arrangorflate/arrangor/:orgnr/gjennomforing",
    () => {
      const gjennomforinger = [
        {
          id: "ded95e13-c121-45b1-a6b7-beadd85e2aa1",
          navn: "AFT Foobar",
          startDato: "2022-04-01",
          sluttDato: "2026-11-26",
        },
        {
          id: "90ae5baf-98b0-49d3-803f-1bd9d8e1a719",
          navn: "AFT høstblad",
          startDato: "2024-04-01",
          sluttDato: "2027-04-01",
        },
      ];
      return HttpResponse.json(gjennomforinger);
    },
  ),
  http.get<PathParams, ArrFlateUtbetaling[]>(
    "*/api/v1/intern/arrangorflate/tilsagn/:id",
    ({ params }) => {
      const { id } = params;
      return HttpResponse.json(arrangorflateTilsagn.find((k) => k.id === id));
    },
  ),
  http.get<PathParams, Arrangor[]>("*/api/v1/intern/arrangorflate/tilgang-arrangor", () =>
    HttpResponse.json([arrangorMock]),
  ),
];
