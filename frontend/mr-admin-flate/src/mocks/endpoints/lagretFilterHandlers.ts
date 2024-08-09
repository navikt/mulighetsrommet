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
        {
          id: window.crypto.randomUUID(),
          brukerId: "B123456",
          filter: {},
          navn:
            "Et mocket tiltaksgjennomføringsfilter med et ganske langt navn så vi kan teste" +
            " hvordan det ser ut også",
          sortOrder: 0,
          type: LagretDokumenttype.TILTAKSGJENNOMFØRING,
        },
        {
          id: window.crypto.randomUUID(),
          brukerId: "B123456",
          filter: {},
          navn: "Et mocket modia/nav-filter",
          sortOrder: 0,
          type: LagretDokumenttype.TILTAKSGJENNOMFØRING_MODIA,
        },
      ]);
    },
  ),
];
