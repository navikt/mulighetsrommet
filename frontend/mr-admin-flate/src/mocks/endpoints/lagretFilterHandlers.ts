import { http, HttpResponse, PathParams } from "msw";
import { LagretDokumenttype, LagretFilter } from "mulighetsrommet-api-client";

export const lagretFilterHandlers = [
  http.get<PathParams, LagretFilter[] | undefined>(
    "*/api/v1/intern/lagret-filter/mine/:dokumenttype",
    () => {
      return HttpResponse.json<LagretFilter[]>([
        {
          id: window.crypto.randomUUID(),
          brukerId: "B123456",
          filter: {},
          navn: "Et mocket-filter",
          sortOrder: 0,
          type: LagretDokumenttype.AVTALE,
        },
      ]);
    },
  ),
];
