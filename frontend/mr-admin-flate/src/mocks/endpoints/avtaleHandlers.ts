import { DefaultBodyType, PathParams, rest } from "msw";
import {
  Avtale,
  AvtaleRequest,
  PaginertAvtale,
  SletteAvtale,
} from "mulighetsrommet-api-client";
import { mockAvtaler } from "../fixtures/mock_avtaler";

export const avtaleHandlers = [
  rest.get<DefaultBodyType, PathParams, PaginertAvtale | undefined>(
    "*/api/v1/internal/avtaler",
    (req, res, ctx) => {
      const avtalestatus = req.url.searchParams.get("avtalestatus");

      return res(
        ctx.status(200),
        ctx.json({
          ...mockAvtaler,
          data: mockAvtaler.data.filter(
            (a) => a.avtalestatus === avtalestatus || avtalestatus === null,
          ),
        }),
      );
    },
  ),

  rest.get<DefaultBodyType, PathParams, PaginertAvtale | undefined>(
    "*/api/v1/internal/avtaler/mine",
    (req, res, ctx) => {
      const avtalestatus = req.url.searchParams.get("avtalestatus");
      const brukerident = "B123456";

      return res(
        ctx.status(200),
        ctx.json({
          ...mockAvtaler,
          data: mockAvtaler.data.filter(
            (a) =>
              (a.avtalestatus === avtalestatus || avtalestatus === null) &&
              a.ansvarlig?.navident === brukerident,
          ),
        }),
      );
    },
  ),

  rest.get<DefaultBodyType, PathParams, Avtale | undefined>(
    "*/api/v1/internal/avtaler/:id",
    (req, res, ctx) => {
      const { id } = req.params as { id: string };
      const avtale = mockAvtaler.data.find((a) => a.id === id) ?? undefined;
      return res(ctx.status(200), ctx.json(avtale));
    },
  ),

  rest.get<DefaultBodyType, PathParams, Avtale | undefined>(
    "*/api/v1/internal/avtaler/skjema",
    (req, res, ctx) => {
      const { id } = req.params as { id: string };
      const avtale = mockAvtaler.data.find((a) => a.id === id) ?? undefined;
      return res(ctx.status(200), ctx.json(avtale));
    },
  ),

  rest.delete<SletteAvtale>("/api/v1/internal/avtaler/:id", (req, res, ctx) => {
    const responsErOk = Math.random() > 0.5;
    if (responsErOk) {
      return res(
        ctx.status(200),
        ctx.json<SletteAvtale>({
          statusCode: 200,
          message: "Avtalen ble slettet",
        }),
      );
    }

    const responses = [
      "Avtalen er mellom start- og sluttdato og må avsluttes før den kan slettes.",
      "Avtalen har 3 tiltaksgjennomføringer koblet til seg. Du må frikoble gjennomføringene før du kan slette avtalen.",
    ];
    const randomIndex = Math.floor(Math.random() * responses.length);

    return res(
      ctx.status(200),
      ctx.json<SletteAvtale>({
        statusCode: 400,
        message: responses[randomIndex],
      }),
    );
  }),

  rest.put<AvtaleRequest>("*/api/v1/internal/avtaler", (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.json({ id: "d1f163b7-1a41-4547-af16-03fd4492b7ba" }),
    );
  }),
];
