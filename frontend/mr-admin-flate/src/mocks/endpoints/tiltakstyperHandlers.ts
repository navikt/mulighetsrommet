import { http, HttpResponse, PathParams } from "msw";
import { TiltakstypeDto, VeilederflateTiltakstype } from "@tiltaksadministrasjon/api-client";
import {
  mockVeilederflateTiltakstypeAFT,
  paginertMockTiltakstyper,
} from "@/mocks/fixtures/mock_tiltakstyper";

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

  http.get<{ id: string }, VeilederflateTiltakstype | undefined>(
    "*/api/tiltaksadministrasjon/tiltakstyper/:id/faneinnhold",
    () => {
      return HttpResponse.json(mockVeilederflateTiltakstypeAFT);
    },
  ),
];
