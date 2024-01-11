import { http, HttpResponse, PathParams } from "msw";
import {
  DelMedBruker,
  DialogResponse,
  GetAlleDeltMedBrukerRequest,
  GetDelMedBrukerRequest,
} from "mulighetsrommet-api-client";
import { mockDeltMedBruker } from "../../fixtures/mockDeltMedBruker";

export const delMedBrukerHandlers = [
  http.put<PathParams, DelMedBruker, DelMedBruker>(
    "*/api/v1/internal/del-med-bruker",
    async ({ request }) => {
      const data = (await request.json()) as DelMedBruker;
      return HttpResponse.json(data);
    },
  ),

  http.post<PathParams, GetDelMedBrukerRequest>(
    "*/api/v1/internal/del-med-bruker",
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
    "*/api/v1/internal/del-med-bruker/alle",
    () => {
      return HttpResponse.json(mockDeltMedBruker);
    },
  ),

  http.post<PathParams, DialogResponse>("*/api/v1/internal/dialog", () =>
    HttpResponse.json({
      id: "12345",
    }),
  ),
];
