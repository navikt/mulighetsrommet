import { http, HttpResponse, PathParams } from "msw";
import { LagretDokumenttype, LagretFilterDto } from "@mr/api-client-v2";

export const lagretFilterHandlers = [
  http.get<PathParams, LagretFilterDto[] | undefined>(
    "*/api/v1/intern/lagret-filter/mine/:dokumenttype",
    () => {
      return HttpResponse.json<LagretFilterDto[]>([
        {
          id: window.crypto.randomUUID(),
          filter: {},
          navn: "Et mocket-filter",
          isDefault: false,
          sortOrder: 0,
          type: LagretDokumenttype.AVTALE,
        },
        {
          id: window.crypto.randomUUID(),
          filter: {},
          navn:
            "Et mocket tiltaksgjennomføringsfilter med et ganske langt navn så vi kan teste" +
            " hvordan det ser ut også",
          isDefault: false,
          sortOrder: 0,
          type: LagretDokumenttype.GJENNOMFORING,
        },
        {
          id: window.crypto.randomUUID(),
          filter: {},
          navn: "Et mocket modia/nav-filter",
          isDefault: false,
          sortOrder: 0,
          type: LagretDokumenttype.GJENNOMFORING_MODIA,
        },
      ]);
    },
  ),
];
