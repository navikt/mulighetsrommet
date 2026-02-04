import { http, HttpResponse } from "msw";
import { ArrangorflateUtbetalingStatus } from "api-client";
import {
  utbetalingTabellOversiktAktive,
  utbetalingTabellOversiktHistoriske,
} from "./utbetalingOversiktMocks";
import { arrFlateUtbetaling, klarForGodkjenningIds } from "./utbetalingDetaljerMocks";
import { arrangorflateTilsagn, tilsagnRader } from "./tilsagnMocks";
import { handlers as opprettKravHandlers } from "./opprettKrav/handlers";

const API_BASE = "*/api/arrangorflate";

function apiHandler(
  method: "get" | "post" | "put" | "delete",
  path: string,
  handler: Parameters<typeof http.get>[1],
) {
  return [http[method](`${API_BASE}${path}`, handler as any)];
}

export const handlers = [
  ...apiHandler("post", "/vedlegg/scan", () => HttpResponse.json(true)),
  ...apiHandler("get", "/utbetaling", ({ request }) => {
    const type = new URL(request.url).searchParams.get("type");
    if (type === "AKTIVE") {
      return HttpResponse.json(utbetalingTabellOversiktAktive);
    }
    return HttpResponse.json(utbetalingTabellOversiktHistoriske);
  }),
  ...apiHandler("get", "/utbetaling/:id", ({ params }) => {
    const { id } = params;
    const utbetaling = arrFlateUtbetaling.find((k) => k.id === id);
    if (utbetaling?.id && klarForGodkjenningIds.includes(utbetaling.id)) {
      return HttpResponse.json({
        ...utbetaling,
        status: ArrangorflateUtbetalingStatus.BEHANDLES_AV_NAV,
        godkjentAvArrangorTidspunkt: new Date().toISOString(),
        innsendtAvArrangorDato: new Date().toISOString().split("T")[0],
      });
    }
    return HttpResponse.json(utbetaling);
  }),
  ...apiHandler("get", "/utbetaling/:id/sync-kontonummer", () => {
    const kontoNr = "12345678901";
    const expires = new Date(new Date().getTime() + 5 * 60000).toISOString();
    return HttpResponse.text(kontoNr, {
      headers: { Expires: expires },
    });
  }),
  ...apiHandler("post", "/utbetaling/:id/godkjenn", () => HttpResponse.json({})),
  ...apiHandler("get", "/:orgnr/utbetaling/:id/kvittering", () =>
    HttpResponse.json(undefined, { status: 501 }),
  ),
  ...apiHandler("get", "/utbetaling/:id/tilsagn", ({ params }) => {
    const { id } = params;
    const utbetaling = arrFlateUtbetaling.find((u) => u.id === id);
    return HttpResponse.json(
      arrangorflateTilsagn.filter((it) => it.gjennomforing.id === utbetaling?.gjennomforing.id),
    );
  }),
  ...apiHandler("get", "/utbetaling/:id/pdf", () =>
    HttpResponse.json(
      { type: "method-not-allowed", title: "PDF er ikke tilgjengelig i Demo", status: "405" },
      { status: 405 },
    ),
  ),
  ...apiHandler("get", "/arrangor/:orgnr/features", ({ request }) => {
    const query = new URL(request.url).searchParams;
    const toggleEnabled = !query
      .getAll("feature")
      .includes("ARRANGORFLATE_OPPRETT_UTBETALING_ANNEN_AVTALT_PPRIS");
    return HttpResponse.json(toggleEnabled);
  }),
  ...apiHandler("get", "/tilsagn", () => {
    return HttpResponse.json({ tabell: tilsagnRader });
  }),
  ...apiHandler("get", "/tilsagn/:id", ({ params }) => {
    const { id } = params;
    return HttpResponse.json(arrangorflateTilsagn.find((k) => k.id === id));
  }),
  ...opprettKravHandlers,
];
