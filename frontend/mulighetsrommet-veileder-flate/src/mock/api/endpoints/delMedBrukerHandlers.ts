import {
  DeltMedBrukerDto,
  DelTiltakMedBrukerResponse,
  GetAlleDeltMedBrukerRequest,
  GetDelMedBrukerRequest,
  TiltakDeltMedBrukerDto,
} from "@api-client";
import { http, HttpResponse, PathParams } from "msw";
import { mockDeltMedBruker } from "@/mock/fixtures/mockDeltMedBruker";
import { mockHistorikkDeltMedBruker } from "@/mock/fixtures/mockHistorikkDeltMedBruker";

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
        (deltMedBruker) => deltMedBruker.tiltakId === data.tiltakId,
      );

      if (deltMedBruker) {
        return HttpResponse.json(deltMedBruker);
      } else {
        return HttpResponse.text(null, { status: 204 });
      }
    },
  ),

  http.post<PathParams, GetAlleDeltMedBrukerRequest, DeltMedBrukerDto[]>(
    "*/api/veilederflate/del-med-bruker/alle",
    () => {
      return HttpResponse.json(mockDeltMedBruker);
    },
  ),

  http.post<PathParams, GetAlleDeltMedBrukerRequest, TiltakDeltMedBrukerDto[]>(
    "*/api/veilederflate/del-med-bruker/historikk",
    () => {
      return HttpResponse.json(mockHistorikkDeltMedBruker);
    },
  ),
];
