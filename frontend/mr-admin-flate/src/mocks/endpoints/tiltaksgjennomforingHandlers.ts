import { DefaultBodyType, PathParams, rest } from "msw";
import {
  PaginertTiltaksgjennomforing,
  Tiltaksgjennomforing,
} from "mulighetsrommet-api-client";
import { mockTiltaksgjennomforinger } from "../fixtures/mock_tiltaksgjennomforinger";

export const tiltaksgjennomforingHandlers = [
  rest.get<
    DefaultBodyType,
    PathParams,
    PaginertTiltaksgjennomforing | { x: string }
  >("*/api/v1/internal/tiltaksgjennomforinger", (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(mockTiltaksgjennomforinger));
  }),

  rest.get<
    DefaultBodyType,
    PathParams,
    PaginertTiltaksgjennomforing | { x: string }
  >("*/api/v1/internal/tiltaksgjennomforinger/mine", (req, res, ctx) => {
    const brukerident = "B123456";
    const data = mockTiltaksgjennomforinger.data.filter(
      (gj) => gj.ansvarlig?.navident === brukerident,
    );
    return res(
      ctx.status(200),
      ctx.json({
        pagination: {
          pageSize: 15,
          currentPage: 1,
          totalCount: data.length,
        },
        data,
      }),
    );
  }),

  rest.put<DefaultBodyType, PathParams, Tiltaksgjennomforing>(
    "*/api/v1/internal/tiltaksgjennomforinger",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockTiltaksgjennomforinger.data[0]));
    },
  ),

  rest.get<DefaultBodyType, PathParams, Tiltaksgjennomforing | undefined>(
    "*/api/v1/internal/tiltaksgjennomforinger/skjema",
    (req, res, ctx) => {
      const { id } = req.params as { id: string };
      const avtale =
        mockTiltaksgjennomforinger.data.find((a) => a.id === id) ?? undefined;
      return res(ctx.status(200), ctx.json(avtale));
    },
  ),

  rest.get<DefaultBodyType, PathParams, Tiltaksgjennomforing[]>(
    "*/api/v1/internal/tiltaksgjennomforinger/sok",
    (req, res, ctx) => {
      const tiltaksnummer = req.url.searchParams.get("tiltaksnummer");

      if (!tiltaksnummer) {
        throw new Error("Tiltaksnummer er ikke satt som query-param");
      }

      const gjennomforing = mockTiltaksgjennomforinger.data.filter((tg) =>
        tg.tiltaksnummer.toString().includes(tiltaksnummer),
      );

      return res(ctx.status(200), ctx.json(gjennomforing));
    },
  ),

  rest.get<DefaultBodyType, { id: string }, Tiltaksgjennomforing | undefined>(
    "*/api/v1/internal/tiltaksgjennomforinger/:id",
    (req, res, ctx) => {
      const { id } = req.params;

      const gjennomforing = mockTiltaksgjennomforinger.data.find(
        (gj) => gj.id === id,
      );
      if (!gjennomforing) {
        return res(ctx.status(404), ctx.json(undefined));
      }

      return res(ctx.status(200), ctx.json(gjennomforing));
    },
  ),

  rest.put<DefaultBodyType, { id: string }, Number>(
    "*/api/v1/internal/tiltaksgjennomforinger/:id/avbryt",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(1));
    },
  ),

  rest.get<
    DefaultBodyType,
    { id: string },
    PaginertTiltaksgjennomforing | undefined
  >(
    "*/api/v1/internal/tiltaksgjennomforinger/tiltakstype/:id",
    (req, res, ctx) => {
      const { id } = req.params as { id: string };

      const gjennomforinger = mockTiltaksgjennomforinger.data.filter(
        (gj) => gj.tiltakstype.id === id,
      );
      if (!gjennomforinger) {
        return res(ctx.status(404), ctx.json(undefined));
      }

      return res(
        ctx.status(200),

        ctx.json({
          pagination: {
            totalCount: gjennomforinger.length,
            currentPage: 1,
            pageSize: 50,
          },
          data: gjennomforinger,
        }),
      );
    },
  ),

  rest.get<
    DefaultBodyType,
    { tiltakskode: string },
    PaginertTiltaksgjennomforing
  >(
    "*/api/v1/internal/tiltaksgjennomforinger/tiltakskode/:tiltakskode",
    (req, res, ctx) => {
      const { tiltakskode } = req.params;
      const gjennomforinger = mockTiltaksgjennomforinger.data.filter(
        (gj) => gj.tiltakstype.arenaKode === tiltakskode,
      );
      return res(
        ctx.status(200),

        ctx.json({
          pagination: {
            totalCount: gjennomforinger.length,
            currentPage: 1,
            pageSize: 50,
          },
          data: gjennomforinger,
        }),
      );
    },
  ),

  rest.get<DefaultBodyType, { enhet: string }, PaginertTiltaksgjennomforing>(
    "*/api/v1/internal/tiltaksgjennomforinger/enhet/:enhet",
    (req, res, ctx) => {
      const { enhet } = req.params;
      const gjennomforinger = mockTiltaksgjennomforinger.data.filter(
        (gj) => gj.arenaAnsvarligEnhet === enhet,
      );
      return res(
        ctx.status(200),

        ctx.json({
          pagination: {
            totalCount: gjennomforinger.length,
            currentPage: 1,
            pageSize: 50,
          },
          data: gjennomforinger,
        }),
      );
    },
  ),
];
