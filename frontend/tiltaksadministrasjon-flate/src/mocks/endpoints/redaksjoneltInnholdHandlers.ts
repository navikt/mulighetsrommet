import { http, HttpResponse } from "msw";
import { RedaksjoneltInnholdLenke } from "@tiltaksadministrasjon/api-client";
import { redaksjoneltInnholdLenker } from "@/mocks/fixtures/mock_redaksjoneltInnhold";

export const redaksjoneltInnholdHandlers = [
  http.get("*/api/tiltaksadministrasjon/redaksjonelt-innhold/lenker", () => {
    return HttpResponse.json(redaksjoneltInnholdLenker);
  }),

  http.put<{ id: string }, RedaksjoneltInnholdLenke>(
    "*/api/tiltaksadministrasjon/redaksjonelt-innhold/lenker/:id",
    async () => {
      return HttpResponse.json(redaksjoneltInnholdLenker[0]);
    },
  ),

  http.delete<{ id: string }>("*/api/tiltaksadministrasjon/redaksjonelt-innhold/lenker/:id", () => {
    return new HttpResponse(null, { status: 204 });
  }),
];
