import { HttpResponse, PathParams, http } from "msw";
import {
  PaginertTiltakstype,
  Tiltakstype,
  PaginertAvtale,
  VeilederflateTiltakstype,
} from "mulighetsrommet-api-client";
import {
  paginertMockTiltakstyper,
  mockVeilederflateTiltakstypeAFT,
  mockMigrerteTiltakstyper,
} from "../fixtures/mock_tiltakstyper";
import { mockAvtaler } from "../fixtures/mock_avtaler";

export const tiltakstypeHandlers = [
  http.get<PathParams, PaginertTiltakstype>("*/api/v1/internal/tiltakstyper", () => {
    return HttpResponse.json(paginertMockTiltakstyper);
  }),

  http.get<PathParams, string[]>("*/api/v1/internal/tiltakstyper/migrerte", () => {
    return HttpResponse.json(mockMigrerteTiltakstyper);
  }),

  http.get<{ id: string }, Tiltakstype | undefined>(
    "*/api/v1/internal/tiltakstyper/:id",
    ({ params }) => {
      const { id } = params;
      return HttpResponse.json(paginertMockTiltakstyper.data.find((gj) => gj.id === id));
    },
  ),

  http.get<{ id: string }, VeilederflateTiltakstype | undefined>(
    "*/api/v1/internal/tiltakstyper/:id/faneinnhold",
    () => {
      return HttpResponse.json(mockVeilederflateTiltakstypeAFT);
    },
  ),

  http.get<{ id: string }, PaginertAvtale>(
    "*/api/v1/internal/avtaler/tiltakstype/:id",
    ({ params }) => {
      const { id } = params;
      const avtaler = mockAvtaler.filter((a) => a.tiltakstype.id === id) ?? [];

      HttpResponse.json({
        pagination: {
          currentPage: 1,
          pageSize: 50,
          totalCount: avtaler.length,
        },
        data: avtaler,
      });
    },
  ),
];
