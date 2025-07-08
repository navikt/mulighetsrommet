import { http, HttpResponse, PathParams } from "msw";
import { Arrangor, ArrFlateUtbetaling } from "api-client";
import { mockArrFlateUtbetalingKompakt } from "./utbetalingOversiktMocks";
import { arrFlateUtbetaling } from "./utbetalingDetaljerMocks";
import { v4 as uuid } from "uuid";
import { arrangorflateTilsagn } from "./tilsagnMocks";

const arrangorMock: Arrangor = {
  id: uuid(),
  organisasjonsnummer: "123456789",
  organisasjonsform: "AS",
  navn: "Arrangør",
  overordnetEnhet: null,
};

export const handlers = [
  http.get<PathParams, ArrFlateUtbetaling[]>(
    "*/api/v1/intern/arrangorflate/arrangor/:orgnr/utbetaling",
    () => HttpResponse.json(mockArrFlateUtbetalingKompakt),
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
    "*/api/v1/intern/arrangorflate/arrangor/:orgnr/tilsagn",
    () => HttpResponse.json(arrangorflateTilsagn),
  ),
  http.get<PathParams, boolean>(
    "*/api/v1/intern/arrangorflate/arrangor/:orgnr/features",
    () => new HttpResponse(true, { status: 200 }),
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
