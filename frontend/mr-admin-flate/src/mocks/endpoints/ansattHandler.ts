import { HttpResponse, PathParams, http } from "msw";
import { NavAnsatt } from "@mr/api-client-v2";
import { mockRedaktor, mockKontaktpersoner } from "../fixtures/mock_ansatt";

export const ansattHandlers = [
  http.get<PathParams, NavAnsatt[]>("*/api/v1/intern/ansatt", ({ request }) => {
    const url = new URL(request.url);
    const roller = url.searchParams.getAll("roller");
    return HttpResponse.json(
      mockKontaktpersoner.filter((k) => k.roller.some((r) => roller.includes(r))),
    );
  }),

  http.get<PathParams, NavAnsatt>("*/api/v1/intern/ansatt/me", () =>
    HttpResponse.json(mockRedaktor),
  ),
];
