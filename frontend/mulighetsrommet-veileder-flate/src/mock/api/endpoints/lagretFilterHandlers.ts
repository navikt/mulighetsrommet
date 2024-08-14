import { http, HttpResponse, PathParams } from "msw";
import { LagretDokumenttype, LagretFilter } from "@mr/api-client";

export const lagretFilterHandlers = [
  http.get<PathParams, LagretFilter[] | undefined>(
    "*/api/v1/intern/lagret-filter/mine/:dokumenttype",
    () => {
      return HttpResponse.json<LagretFilter[]>([
        {
          id: "cd5ed640-e8e4-46fb-a7b5-e98667f6c1ab",
          brukerId: "Z990079",
          navn: "Varig tilpasset Fredrikstad",
          type: LagretDokumenttype.TILTAKSGJENNOMFØRING_MODIA,
          filter: {
            search: "",
            navEnheter: [
              {
                navn: "NAV Fredrikstad",
                type: "LOKAL",
                status: "AKTIV",
                enhetsnummer: "0106",
                overordnetEnhet: "0200",
              },
            ],
            tiltakstyper: [],
            innsatsgruppe: {
              nokkel: "VARIG_TILPASSET_INNSATS",
              tittel: "Varig tilpasset innsats",
            },
            apentForInnsok: "APENT_ELLER_STENGT",
          },
          sortOrder: 0,
        },
      ]);
    },
  ),
];
