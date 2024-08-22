import { http, HttpResponse, PathParams } from "msw";
import {
  DelMedBruker,
  DelTiltakMedBrukerResponse,
  GetAlleDeltMedBrukerRequest,
  GetDelMedBrukerRequest,
  TiltakDeltMedBruker,
} from "@mr/api-client";
import { mockDeltMedBruker } from "../../fixtures/mockDeltMedBruker";
import { mockHistorikkDeltMedBruker } from "../../fixtures/mockHistorikkDeltMedBruker";

export const delMedBrukerHandlers = [
  http.post<PathParams, DelTiltakMedBrukerResponse>("*/api/v1/intern/del-med-bruker", () => {
    const body: DelTiltakMedBrukerResponse = {
      dialogId: "12345",
    };
    return HttpResponse.json(body);
  }),

  http.post<PathParams, GetDelMedBrukerRequest>(
    "*/api/v1/intern/del-med-bruker/status",
    async ({ request }) => {
      const data = (await request.json()) as GetDelMedBrukerRequest;

      const deltMedBruker = mockDeltMedBruker.find(
        (delt) => delt.sanityId === data.tiltakId || delt.tiltaksgjennomforingId === data.tiltakId,
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
];
