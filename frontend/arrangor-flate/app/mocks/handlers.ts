import { DefaultBodyType, http, HttpResponse, PathParams } from "msw";
import {
  ArrangorflateTilsagnOversikt,
  ArrangorflateUtbetalingDto,
  ArrangorflateUtbetalingerOversikt,
  ArrangorflateUtbetalingStatus,
} from "api-client";
import {
  utbetalingTabellOversiktAktive,
  utbetalingTabellOversiktHistoriske,
} from "./utbetalingOversiktMocks";
import { arrFlateUtbetaling, klarForGodkjenningIds } from "./utbetalingDetaljerMocks";
import { arrangorflateTilsagn, tilsagnOversikt } from "./tilsagnMocks";
import { handlers as opprettKravHandlers } from "./opprettKrav/handlers";

export const handlers = [
  http.post<PathParams, DefaultBodyType>("*/api/arrangorflate/vedlegg/scan", () =>
    HttpResponse.json(true),
  ),
  http.get<PathParams, ArrangorflateUtbetalingerOversikt>(
    "*/api/arrangorflate/utbetaling",
    ({ request }) => {
      const type = new URL(request.url).searchParams.get("type");
      if (type === "AKTIVE") {
        return HttpResponse.json({ tabell: utbetalingTabellOversiktAktive });
      }
      return HttpResponse.json({ tabell: utbetalingTabellOversiktHistoriske });
    },
  ),
  http.get<PathParams, ArrangorflateUtbetalingDto[]>(
    "*/api/arrangorflate/utbetaling/:id",
    ({ params }) => {
      const { id } = params;
      const utbetaling = arrFlateUtbetaling.find((k) => k.id === id);
      if (utbetaling?.id && klarForGodkjenningIds.includes(utbetaling.id)) {
        return HttpResponse.json({
          ...utbetaling,
          status: ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV,
          godkjentAvArrangorTidspunkt: new Date().toISOString(),
        });
      }
      return HttpResponse.json(utbetaling);
    },
  ),
  http.get<PathParams, string>("*/api/arrangorflate/utbetaling/:id/sync-kontonummer", () => {
    const kontoNr = "12345678901";
    const expires = new Date(new Date().getTime() + 5 * 60000).toISOString(); // 5 min, NS_BINDING_ABORTED fix
    return HttpResponse.text(kontoNr, {
      headers: { Expires: expires },
    });
  }),
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
    "*/api/arrangorflate/utbetaling/:id/utbetalingsdetaljer",
    () =>
      HttpResponse.json(
        { type: "method-not-allowed", title: "PDF er ikke tilgjengelig i Demo", status: "405" },
        { status: 405 },
      ),
  ),
  http.get<PathParams, boolean>("*/api/arrangorflate/arrangor/:orgnr/features", ({ request }) => {
    const query = new URL(request.url).searchParams;
    const toggleEnabled = !query
      .getAll("feature")
      .includes("ARRANGORFLATE_OPPRETT_UTBETALING_ANNEN_AVTALT_PPRIS");
    return HttpResponse.json(toggleEnabled);
  }),
  http.get<PathParams, ArrangorflateTilsagnOversikt>("*/api/arrangorflate/tilsagn", () => {
    return HttpResponse.json({ tabell: tilsagnOversikt });
  }),
  http.get<PathParams, ArrangorflateUtbetalingDto[]>(
    "*/api/arrangorflate/tilsagn/:id",
    ({ params }) => {
      const { id } = params;
      return HttpResponse.json(arrangorflateTilsagn.find((k) => k.id === id));
    },
  ),
  ...opprettKravHandlers,
];
