import { http, HttpResponse, PathParams } from "msw";
import { mockArrangorer } from "../fixtures/mock_arrangorer";
import { BrregHovedenhetDto } from "@tiltaksadministrasjon/api-client";

export const virksomhetHandlers = [
  http.get<{ q: string }, BrregHovedenhetDto[]>("*/api/v1/intern/virksomhet/sok", ({ request }) => {
    const url = new URL(request.url);
    const sok = url.searchParams.get("q");
    if (!sok) {
      return HttpResponse.text("Missing 'sok' parameter", { status: 400 });
    }

    return HttpResponse.json(
      mockArrangorer.data.filter((enhet) =>
        enhet.navn.toLowerCase().includes(sok.toLocaleLowerCase()),
      ),
    );
  }),

  http.get<PathParams>("*/api/v1/intern/virksomhet/:orgnr/underenheter", ({ params }) => {
    return HttpResponse.json(
      mockArrangorer.data.find((enhet) => enhet.organisasjonsnummer === params.orgnr)?.underenheter,
    );
  }),

  http.post<PathParams>("*/api/v1/intern/virksomhet/:orgnr", ({ params }) => {
    return HttpResponse.json(
      mockArrangorer.data.find((enhet) => enhet.organisasjonsnummer === params.orgnr),
    );
  }),
];
