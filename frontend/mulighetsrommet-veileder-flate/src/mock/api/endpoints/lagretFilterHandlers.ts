import { http, HttpResponse, PathParams } from "msw";
import { LagretDokumenttype, LagretFilterDto } from "@mr/api-client-v2";

export const lagretFilterHandlers = [
  http.get<PathParams, LagretFilterDto[] | undefined>(
    "*/api/v1/intern/lagret-filter/mine/:dokumenttype",
    () => {
      return HttpResponse.json<LagretFilterDto[]>([
        {
          id: "cd5ed640-e8e4-46fb-a7b5-e98667f6c1ab",
          navn: "Liten mulighet Fredrikstad",
          type: LagretDokumenttype.GJENNOMFORING_MODIA,
          filter: {
            search: "",
            navEnheter: [
              {
                navn: "Nav Fredrikstad",
                type: "LOKAL",
                status: "AKTIV",
                enhetsnummer: "0106",
                overordnetEnhet: "0200",
              },
            ],
            tiltakstyper: [],
            innsatsgruppe: {
              nokkel: "LITEN_MULIGHET_TIL_A_JOBBE",
              tittel: "Liten mulighet",
            },
            apentForPamelding: "APENT_ELLER_STENGT",
          },
          isDefault: false,
          sortOrder: 0,
        },
      ]);
    },
  ),
];
