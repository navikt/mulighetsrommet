import { http, HttpResponse, PathParams } from "msw";
import { NavAnsattDto } from "@tiltaksadministrasjon/api-client";
import { mockKontaktpersoner, mockRedaktor } from "@/mocks/fixtures/mock_ansatt";

export const ansattHandlers = [
  http.get<PathParams, NavAnsattDto[]>("*/api/tiltaksadministrasjon/ansatt", ({ request }) => {
    const url = new URL(request.url);
    const roller = url.searchParams.getAll("roller");
    return HttpResponse.json(
      mockKontaktpersoner.filter((k) => k.roller.some((dto) => roller.includes(dto.rolle))),
    );
  }),

  http.get<PathParams, NavAnsattDto>("*/api/tiltaksadministrasjon/ansatt/me", () =>
    HttpResponse.json(mockRedaktor),
  ),
];
