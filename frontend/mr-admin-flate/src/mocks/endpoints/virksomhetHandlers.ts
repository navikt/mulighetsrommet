import { http, HttpResponse, PathParams } from "msw";
import { BrregVirksomhet } from "mulighetsrommet-api-client";
import { mockArrangorer } from "../fixtures/mock_arrangorer";

export const virksomhetHandlers = [
  http.get<{ sok: string }, BrregVirksomhet[]>(
    "*/api/v1/internal/virksomhet/sok",
    ({ request }) => {
      const url = new URL(request.url);
      const sok = url.searchParams.get("sok")!!;
      return HttpResponse.json(
        Object.values(mockArrangorer).filter((enhet) =>
          enhet.navn?.toLowerCase().includes(sok.toLocaleLowerCase()),
        ),
      );
    },
  ),
  http.get<PathParams, BrregVirksomhet | undefined>(
    "*/api/v1/internal/virksomhet/:orgnr/underenheter",
    ({ params }) => {
      return HttpResponse.json(
        Object.values(mockArrangorer).find((enhet) => enhet.organisasjonsnummer === params.orgnr)
          ?.underenheter,
      );
    },
  ),
  http.post<PathParams, BrregVirksomhet | undefined>(
    "*/api/v1/internal/virksomhet/:orgnr",
    ({ params }) => {
      return HttpResponse.json(
        Object.values(mockArrangorer).find((enhet) => enhet.organisasjonsnummer === params.orgnr),
      );
    },
  ),
];
