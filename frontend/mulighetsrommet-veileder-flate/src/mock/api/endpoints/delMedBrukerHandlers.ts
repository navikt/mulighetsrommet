import {
  DelMedBrukerDbo as DelMedBruker,
  DelTiltakMedBrukerResponse,
  GetAlleDeltMedBrukerRequest,
  GetDelMedBrukerRequest,
  TiltakDeltMedBruker,
} from "@api-client";
import { http, HttpResponse, PathParams } from "msw";
import { mockDeltMedBruker } from "../../fixtures/mockDeltMedBruker";
import { mockHistorikkDeltMedBruker } from "../../fixtures/mockHistorikkDeltMedBruker";

export const delMedBrukerHandlers = [
  http.post<PathParams, DelTiltakMedBrukerResponse>("*/api/veilederflate/del-med-bruker", () => {
    const body: DelTiltakMedBrukerResponse = {
      dialogId: "12345",
    };
    return HttpResponse.json(body);
  }),

  http.post<PathParams, GetDelMedBrukerRequest>(
    "*/api/veilederflate/del-med-bruker/status",
    async ({ request }) => {
      const data = (await request.json()) as GetDelMedBrukerRequest;

      const deltMedBruker = mockDeltMedBruker.find(
        (delt) => delt.sanityId === data.tiltakId || delt.gjennomforingId === data.tiltakId,
      );

      if (deltMedBruker) {
        return HttpResponse.json(deltMedBruker);
      } else {
        return HttpResponse.text(null, { status: 204 });
      }
    },
  ),

  http.post<PathParams, GetAlleDeltMedBrukerRequest, DelMedBruker[]>(
    "*/api/veilederflate/del-med-bruker/alle",
    () => {
      return HttpResponse.json(mockDeltMedBruker);
    },
  ),

  http.post<PathParams, GetAlleDeltMedBrukerRequest, TiltakDeltMedBruker[]>(
    "*/api/veilederflate/del-med-bruker/historikk",
    () => {
      return HttpResponse.json(mockHistorikkDeltMedBruker);
    },
  ),
];
