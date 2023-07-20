import { DefaultBodyType, PathParams, rest } from "msw";
import {
  PaginertTiltakstype,
  Tiltakstype,
  TiltakstypeNokkeltall,
  PaginertAvtale,
} from "mulighetsrommet-api-client";
import { mockAvtaler } from "../fixtures/mock_avtaler";
import { mockTiltakstyper } from "../fixtures/mock_tiltakstyper";
import { mockTiltakstyperNokkeltall } from "../fixtures/mock_tiltakstyper_nokkeltall";

export const tiltakstypeHandlers = [
  rest.get<DefaultBodyType, PathParams, PaginertTiltakstype>(
    "*/api/v1/internal/tiltakstyper",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockTiltakstyper));
    },
  ),

  rest.get<DefaultBodyType, { id: string }, Tiltakstype | undefined>(
    "*/api/v1/internal/tiltakstyper/:id",
    (req, res, ctx) => {
      const { id } = req.params;
      return res(
        ctx.status(200),

        ctx.json(mockTiltakstyper.data.find((gj) => gj.id === id)),
      );
    },
  ),
  rest.get<DefaultBodyType, { id: string }, TiltakstypeNokkeltall | undefined>(
    "*/api/v1/internal/tiltakstyper/:id/nokkeltall",
    (req, res, ctx) => {
      return res(
        ctx.status(200),

        ctx.json(mockTiltakstyperNokkeltall),
      );
    },
  ),

  rest.get<DefaultBodyType, { id: string }, PaginertAvtale>(
    "*/api/v1/internal/avtaler/tiltakstype/:id",
    (req, res, ctx) => {
      const { id } = req.params as { id: string };
      const avtaler =
        mockAvtaler.data.filter((a) => a.tiltakstype.id === id) ?? [];
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
