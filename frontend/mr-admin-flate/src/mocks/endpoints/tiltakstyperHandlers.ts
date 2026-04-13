import { http, HttpResponse, PathParams } from "msw";
import { TiltakstypeDto } from "@tiltaksadministrasjon/api-client";
import { paginertMockTiltakstyper } from "@/mocks/fixtures/mock_tiltakstyper";

export const tiltakstypeHandlers = [
  http.get<PathParams, TiltakstypeDto[]>("*/api/tiltaksadministrasjon/tiltakstyper", () => {
    return HttpResponse.json(paginertMockTiltakstyper);
  }),

  http.get<{ id: string }, TiltakstypeDto | undefined>(
    "*/api/tiltaksadministrasjon/tiltakstyper/:id",
    ({ params }) => {
      const { id } = params;
      return HttpResponse.json(paginertMockTiltakstyper.find((gj) => gj.id === id));
    },
  ),

  http.patch<{ id: string }>(
    "*/api/tiltaksadministrasjon/tiltakstyper/:id/redaksjonelt-innhold",
    () => {
      return new HttpResponse(null, { status: 200 });
    },
  ),
];
