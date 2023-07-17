import { rest } from "msw";
import {
  PaginertTiltaksgjennomforing,
  Tiltaksgjennomforing,
  TiltaksgjennomforingNokkeltall,
} from "mulighetsrommet-api-client";
import { mockTiltaksgjennomforinger } from "../fixtures/mock_tiltaksgjennomforinger";
import { mockTiltaksgjennomforingerNokkeltall } from "../fixtures/mock_tiltaksgjennomforinger_nokkeltall";

export const tiltaksgjennomforingHandlers = [
  rest.get<any, any, PaginertTiltaksgjennomforing | { x: string }>(
    "*/api/v1/internal/tiltaksgjennomforinger",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockTiltaksgjennomforinger));
    },
  ),

  rest.put<any, any, Tiltaksgjennomforing>(
    "*/api/v1/internal/tiltaksgjennomforinger",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockTiltaksgjennomforinger.data[0]));
    },
  ),

  rest.get<any, any, Tiltaksgjennomforing[]>(
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
  rest.get<any, { id: string }, TiltaksgjennomforingNokkeltall | undefined>(
    "*/api/v1/internal/tiltaksgjennomforinger/:id/nokkeltall",
    (req, res, ctx) => {
      return res(
        ctx.status(200),

        ctx.json(mockTiltaksgjennomforingerNokkeltall),
      );
    },
  ),

  rest.get<any, { id: string }, Tiltaksgjennomforing | undefined>(
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

  rest.put<any, { id: string }, Number>(
    "*/api/v1/internal/tiltaksgjennomforinger/:id/avbryt",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(1));
    },
  ),

  rest.get<any, { id: string }, PaginertTiltaksgjennomforing | undefined>(
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

  rest.get<any, { tiltakskode: string }, PaginertTiltaksgjennomforing>(
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

  rest.get<any, { enhet: string }, PaginertTiltaksgjennomforing>(
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
