import { DefaultBodyType, http, HttpResponse, PathParams } from "msw";
import {
  ArrangorflateArrangor,
  ArrangorflateUtbetalingDto,
  ArrangorflateUtbetalingKompaktDto,
  ArrangorflateUtbetalingStatus,
} from "api-client";
import { mockArrangorflateUtbetalingKompakt } from "./utbetalingOversiktMocks";
import { arrFlateUtbetaling, klarForGodkjenningIds } from "./utbetalingDetaljerMocks";
import { arrangorflateTilsagn } from "./tilsagnMocks";
import { handlers as opprettKravHandlers } from "./opprettKrav/handlers";
import { arrangorMock } from "./opprettKrav/gjennomforingMocks";

function isAktiv(utbetaling: ArrangorflateUtbetalingKompaktDto): boolean {
  switch (utbetaling.status) {
    case ArrangorflateUtbetalingStatus.KLAR_FOR_GODKJENNING:
    case ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV:
    case ArrangorflateUtbetalingStatus.KREVER_ENDRING:
      return true;
    case ArrangorflateUtbetalingStatus.OVERFORT_TIL_UTBETALING:
    case ArrangorflateUtbetalingStatus.UTBETALT:
      return false;
  }
}

export const handlers = [
  http.post<PathParams, DefaultBodyType>("*/api/arrangorflate/vedlegg/scan", () =>
    HttpResponse.json(true),
  ),
  http.get<PathParams, ArrangorflateUtbetalingDto[]>(
    "*/api/arrangorflate/arrangor/:orgnr/utbetaling",
    () =>
      HttpResponse.json({
        aktive: mockArrangorflateUtbetalingKompakt.filter((u) => isAktiv(u)),
        historiske: mockArrangorflateUtbetalingKompakt.filter((u) => !isAktiv(u)),
        kanOppretteManueltKrav: true,
      }),
  ),
  http.get<PathParams, ArrangorflateUtbetalingDto[]>(
    "*/api/arrangorflate/arrangor/:orgnr/kontonummer",
    () => HttpResponse.text("10002427740"), // random kontonr
  ),
  http.post<PathParams, ArrangorflateUtbetalingDto[]>(
    "*/api/arrangorflate/arrangor/:orgnr/utbetaling",
    () => HttpResponse.text("585a2834-338a-4ac7-82e0-e1b08bfe1408"), // investerings utbetaling
  ),
  http.get<PathParams, ArrangorflateUtbetalingDto[]>(
    "*/api/arrangorflate/utbetaling/:id",
    ({ params }) => {
      const { id } = params;
      const utbetaling = arrFlateUtbetaling.find((k) => k.id === id);
      if (utbetaling?.id && klarForGodkjenningIds.includes(utbetaling.id)) {
        return HttpResponse.json({
          ...utbetaling,
          godkjentAvArrangorTidspunkt: "2025-05-15T11:03:21.959059",
        });
      }
      return HttpResponse.json(utbetaling);
    },
  ),
  http.get<PathParams, string>(
    "*/api/arrangorflate/utbetaling/:id/sync-kontonummer",
    ({ params }) => {
      const { id } = params;
      const kontoNr = arrFlateUtbetaling.find((k) => k.id === id)?.betalingsinformasjon.kontonummer;
      const expires = new Date(new Date().getTime() + 5 * 60000).toISOString(); // 5 min, NS_BINDING_ABORTED fix
      return HttpResponse.text(kontoNr, {
        headers: { Expires: expires },
      });
    },
  ),
  http.post<PathParams, ArrangorflateUtbetalingDto[]>(
    "*/api/arrangorflate/utbetaling/:id/godkjenn",
    () => HttpResponse.json({}),
  ),
  http.get<PathParams, ArrangorflateUtbetalingDto[]>(
    "*/api/arrangorflate/:orgnr/utbetaling/:id/kvittering",
    () => HttpResponse.json(undefined, { status: 501 }),
  ),
  http.get<PathParams, ArrangorflateUtbetalingDto[]>(
    "*/api/arrangorflate/utbetaling/:id/tilsagn",
    ({ params }) => {
      const { id } = params;
      const utbetaling = arrFlateUtbetaling.find((u) => u.id === id);
      return HttpResponse.json(
        arrangorflateTilsagn.filter((it) => it.gjennomforing.id === utbetaling?.gjennomforing.id),
      );
    },
  ),
  http.get<PathParams, ArrangorflateUtbetalingDto[]>(
    "*/api/arrangorflate/utbetaling/:id/relevante-forslag",
    () => {
      return HttpResponse.json([]);
    },
  ),
  http.get<PathParams, ArrangorflateUtbetalingDto[]>(
    "*/api/arrangorflate/utbetaling/:id/utbetalingsdetaljer",
    () =>
      HttpResponse.json(
        { type: "method-not-allowed", title: "PDF er ikke tilgjengelig i Demo", status: "405" },
        { status: 405 },
      ),
  ),
  http.get<PathParams, ArrangorflateUtbetalingDto[]>(
    "*/api/arrangorflate/arrangor/:orgnr/tilsagn",
    () => HttpResponse.json(arrangorflateTilsagn),
  ),
  http.get<PathParams, boolean>("*/api/arrangorflate/arrangor/:orgnr/features", ({ request }) => {
    const query = new URL(request.url).searchParams;
    const toggleEnabled = !query
      .getAll("feature")
      .includes("ARRANGORFLATE_OPPRETT_UTBETALING_ANNEN_AVTALT_PPRIS");
    return HttpResponse.json(toggleEnabled);
  }),
  http.get<PathParams, boolean>("*/api/arrangorflate/arrangor/:orgnr/gjennomforing", () => {
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
  }),
  http.get<PathParams, ArrangorflateUtbetalingDto[]>(
    "*/api/arrangorflate/tilsagn/:id",
    ({ params }) => {
      const { id } = params;
      return HttpResponse.json(arrangorflateTilsagn.find((k) => k.id === id));
    },
  ),
  http.get<PathParams, ArrangorflateArrangor[]>("*/api/arrangorflate/tilgang-arrangor", () =>
    HttpResponse.json([arrangorMock]),
  ),
  http.get("*/api/arrangorflate/:orgnr/features", () => {
    return HttpResponse.json(true);
  }),
  ...opprettKravHandlers,
];
