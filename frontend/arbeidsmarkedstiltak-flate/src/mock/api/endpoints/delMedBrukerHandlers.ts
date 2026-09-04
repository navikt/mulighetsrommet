import {
  DelMedBrukerDto,
  DelTiltakMedBrukerResponse,
  GetAlleDelMedBrukerRequest,
  GetDelMedBrukerRequest,
} from "@arbeidsmarkedstiltak/api-client";
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
        (deltMedBruker) => deltMedBruker.tiltak.id === data.tiltakId,
      );

      if (deltMedBruker) {
        return HttpResponse.json(deltMedBruker);
      } else {
        return HttpResponse.text(null, { status: 204 });
      }
    },
  ),

  http.post<PathParams, GetAlleDelMedBrukerRequest, DelMedBrukerDto[]>(
    "*/api/veilederflate/del-med-bruker/historikk",
    () => {
      return HttpResponse.json(mockHistorikkDeltMedBruker);
    },
  ),
];
