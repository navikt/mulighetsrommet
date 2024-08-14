import { http, HttpResponse, PathParams } from "msw";
import {
  DelMedBruker,
  DialogResponse,
  GetAlleDeltMedBrukerRequest,
  GetDelMedBrukerRequest,
  TiltakDeltMedBruker,
} from "@mr/api-client";
import { mockDeltMedBruker } from "../../fixtures/mockDeltMedBruker";
import { mockHistorikkDeltMedBruker } from "../../fixtures/mockHistorikkDeltMedBruker";

export const delMedBrukerHandlers = [
  http.put<PathParams, DelMedBruker, DelMedBruker>(
    "*/api/v1/intern/del-med-bruker",
    async ({ request }) => {
      const data = (await request.json()) as DelMedBruker;
      return HttpResponse.json(data);
    },
  ),

  http.post<PathParams, GetDelMedBrukerRequest>(
    "*/api/v1/intern/del-med-bruker",
    async ({ request }) => {
      const data = (await request.json()) as GetDelMedBrukerRequest;

      const deltMedBruker = mockDeltMedBruker.find(
        (delt) => delt.sanityId === data.id || delt.tiltaksgjennomforingId === data.id,
      );

      if (deltMedBruker) {
        return HttpResponse.json(deltMedBruker);
      } else {
        return HttpResponse.text(null, { status: 204 });
      }
    },
  ),

  http.post<PathParams, GetAlleDeltMedBrukerRequest, DelMedBruker[]>(
    "*/api/v1/intern/del-med-bruker/alle",
    () => {
      return HttpResponse.json(mockDeltMedBruker);
    },
  ),

  http.post<PathParams, GetAlleDeltMedBrukerRequest, TiltakDeltMedBruker[]>(
    "*/api/v1/intern/del-med-bruker/historikk",
    () => {
      return HttpResponse.json(mockHistorikkDeltMedBruker);
    },
  ),

  http.post<PathParams, DialogResponse>("*/api/v1/intern/dialog", () =>
    HttpResponse.json({
      id: "12345",
    }),
  ),
];
