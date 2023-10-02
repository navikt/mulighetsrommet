import { DefaultBodyType, PathParams, rest } from "msw";
import { Avtale, AvtaleRequest, PaginertAvtale } from "mulighetsrommet-api-client";
import { mockAvtaler } from "../fixtures/mock_avtaler";

export const avtaleHandlers = [
  rest.get<DefaultBodyType, PathParams, PaginertAvtale | undefined>(
    "*/api/v1/internal/avtaler",
    (req, res, ctx) => {
      const avtalestatus = req.url.searchParams.get("avtalestatus");
      const data = mockAvtaler.filter(
        (a) => a.avtalestatus === avtalestatus || avtalestatus === null,
      );

      return res(
        ctx.status(200),
        ctx.json({
          pagination: {
            currentPage: 1,
            pageSize: 15,
            totalCount: data.length,
          },
          data,
        }),
      );
    },
  ),

  rest.get<DefaultBodyType, PathParams, PaginertAvtale | undefined>(
    "*/api/v1/internal/avtaler/mine",
    (req, res, ctx) => {
      const avtalestatus = req.url.searchParams.get("avtalestatus");
      const brukerident = "B123456";
      const data = mockAvtaler.filter(
        (a) =>
          (a.avtalestatus === avtalestatus || avtalestatus === null) &&
          a.administrator?.navIdent === brukerident,
      );

      return res(
        ctx.status(200),
        ctx.json({
          pagination: {
            currentPage: 1,
            pageSize: 15,
            totalCount: data.length,
          },
          data,
        }),
      );
    },
  ),

  rest.get<DefaultBodyType, PathParams, Avtale | undefined>(
    "*/api/v1/internal/avtaler/:id",
    (req, res, ctx) => {
      const { id } = req.params as {
        id: string;
      };
      const avtale = mockAvtaler.find((a) => a.id === id) ?? undefined;
      return res(ctx.status(200), ctx.json(avtale));
    },
  ),

  rest.get<DefaultBodyType, PathParams, Avtale | undefined>(
    "*/api/v1/internal/avtaler/skjema",
    (req, res, ctx) => {
      const { id } = req.params as {
        id: string;
      };
      const avtale = mockAvtaler.find((a) => a.id === id) ?? undefined;
      return res(ctx.status(200), ctx.json(avtale));
    },
  ),

  rest.delete("/api/v1/internal/avtaler/:id", (req, res, ctx) => {
    return res(ctx.status(200));
  }),

  rest.put<AvtaleRequest>("*/api/v1/internal/avtaler", (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({
        id: "d1f163b7-1a41-4547-af16-03fd4492b7ba",
      }),
    );
  }),
];
