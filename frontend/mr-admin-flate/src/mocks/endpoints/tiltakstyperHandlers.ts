import { DefaultBodyType, PathParams, rest } from "msw";
import {
  PaginertTiltakstype,
  Tiltakstype,
  PaginertAvtale,
  VeilederflateTiltakstype,
} from "mulighetsrommet-api-client";
import {
  paginertMockTiltakstyper,
  mockVeilederflateTiltakstypeAFT,
} from "../fixtures/mock_tiltakstyper";
import { mockAvtaler } from "../fixtures/mock_avtaler";

export const tiltakstypeHandlers = [
  rest.get<DefaultBodyType, PathParams, PaginertTiltakstype>(
    "*/api/v1/internal/tiltakstyper",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(paginertMockTiltakstyper));
    },
  ),

  rest.get<DefaultBodyType, { id: string }, Tiltakstype | undefined>(
    "*/api/v1/internal/tiltakstyper/:id",
    (req, res, ctx) => {
      const { id } = req.params;
      return res(
        ctx.status(200),

        ctx.json(paginertMockTiltakstyper.data.find((gj) => gj.id === id)),
      );
    },
  ),

  rest.get<DefaultBodyType, { id: string }, VeilederflateTiltakstype | undefined>(
    "*/api/v1/internal/tiltakstyper/:id/sanity",
    (_req, res, ctx) => {
      return res(
        ctx.status(200),

        ctx.json(mockVeilederflateTiltakstypeAFT),
      );
    },
  ),

  rest.get<DefaultBodyType, { id: string }, PaginertAvtale>(
    "*/api/v1/internal/avtaler/tiltakstype/:id",
    (req, res, ctx) => {
      const { id } = req.params as {
        id: string;
      };
      const avtaler = mockAvtaler.filter((a) => a.tiltakstype.id === id) ?? [];
      return res(
        ctx.status(200),

        ctx.json({
          pagination: {
            currentPage: 1,
            pageSize: 50,
            totalCount: avtaler.length,
          },
          data: avtaler,
        }),
      );
    },
  ),
];
