import { http, HttpResponse, PathParams } from "msw";
import { BrregVirksomhet, ArrangorKontaktperson } from "mulighetsrommet-api-client";
import { mockVirksomhetKontaktperson } from "../fixtures/mock_virksomhet_kontaktperson";
import { mockVirksomheter } from "../fixtures/mock_virksomheter";

export const virksomhetHandlers = [
  http.get<{ sok: string }, BrregVirksomhet[]>(
    "*/api/v1/internal/virksomhet/sok",
    ({ request }) => {
      const url = new URL(request.url);
      const sok = url.searchParams.get("sok")!!;
      return HttpResponse.json(
        Object.values(mockVirksomheter).filter((enhet) =>
          enhet.navn?.toLowerCase().includes(sok.toLocaleLowerCase()),
        ),
      );
    },
  ),
  http.get<PathParams, BrregVirksomhet | undefined>(
    "*/api/v1/internal/virksomhet/:id",
    ({ params }) => {
      return HttpResponse.json(
        Object.values(mockVirksomheter).find((enhet) => enhet.id === params.id),
      );
    },
  ),
  http.get<PathParams, BrregVirksomhet | undefined>(
    "*/api/v1/internal/virksomhet/:orgnr/underenheter",
    ({ params }) => {
      return HttpResponse.json(
        Object.values(mockVirksomheter).find((enhet) => enhet.organisasjonsnummer === params.orgnr)
          ?.underenheter,
      );
    },
  ),
  http.post<PathParams, BrregVirksomhet | undefined>(
    "*/api/v1/internal/virksomhet/:orgnr",
    ({ params }) => {
      return HttpResponse.json(
        Object.values(mockVirksomheter).find((enhet) => enhet.organisasjonsnummer === params.orgnr),
      );
    },
  ),
  http.get<PathParams, BrregVirksomhet[] | undefined>("*/api/v1/internal/virksomhet", () =>
    HttpResponse.json(Object.values(mockVirksomheter)),
  ),

  http.get<PathParams, ArrangorKontaktperson[]>(
    "*/api/v1/internal/virksomhet/*/kontaktpersoner",
    () => HttpResponse.json(mockVirksomhetKontaktperson),
  ),
];
