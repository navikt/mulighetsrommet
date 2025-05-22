import { http, HttpResponse, PathParams } from "msw";
import { LagretFilterType, LagretFilter } from "@mr/api-client-v2";

export const lagretFilterHandlers = [
  http.get<PathParams, LagretFilter[] | undefined>(
    "*/api/v1/intern/lagret-filter/mine/:dokumenttype",
    () => {
      return HttpResponse.json<LagretFilter[]>([
        {
          id: window.crypto.randomUUID(),
          filter: {},
          navn: "Et mocket-filter",
          isDefault: false,
          sortOrder: 0,
          type: LagretFilterType.AVTALE,
        },
        {
          id: window.crypto.randomUUID(),
          filter: {},
          navn:
            "Et mocket tiltaksgjennomføringsfilter med et ganske langt navn så vi kan teste" +
            " hvordan det ser ut også",
          isDefault: false,
          sortOrder: 0,
          type: LagretFilterType.GJENNOMFORING,
        },
        {
          id: window.crypto.randomUUID(),
          filter: {},
          navn: "Et mocket modia/nav-filter",
          isDefault: false,
          sortOrder: 0,
          type: LagretFilterType.GJENNOMFORING_MODIA,
        },
      ]);
    },
  ),
];
