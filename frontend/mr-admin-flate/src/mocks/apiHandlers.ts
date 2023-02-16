import { rest } from "msw";
import {
  Ansatt,
  PaginertAvtale,
  PaginertTiltaksgjennomforing,
  PaginertTiltakstype,
  Tiltaksgjennomforing,
  Tiltakstype,
} from "mulighetsrommet-api-client";
import { mockFagansvarlig, mockTiltaksansvarlig } from "./fixtures/mock_ansatt";
import { mockAvtaler } from "./fixtures/mock_avtaler";
import { mockTiltaksgjennomforinger } from "./fixtures/mock_tiltaksgjennomforinger";
import { mockTiltaksgjennomforingerKobletTilAnsatt } from "./fixtures/mock_tiltaksgjennomforinger_koblet_til_ansatt";
import { mockTiltakstyper } from "./fixtures/mock_tiltakstyper";

export const apiHandlers = [
  rest.get<any, any, PaginertTiltakstype>(
    "*/api/v1/internal/tiltakstyper",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockTiltakstyper));
    }
  ),

  rest.get<any, { id: string }, Tiltakstype | undefined>(
    "*/api/v1/internal/tiltakstyper/:id",
    (req, res, ctx) => {
      const { id } = req.params;
      return res(
        ctx.status(200),

        ctx.json(mockTiltakstyper.data.find((gj) => gj.id === id))
      );
    }
  ),

  rest.get<any, { id: string }, PaginertAvtale>(
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
        })
      );
    }
  ),

  rest.get<any, any, PaginertTiltaksgjennomforing>(
    "*/api/v1/internal/tiltaksgjennomforinger",
    (req, res, ctx) => {
      return res(ctx.status(200), ctx.json(mockTiltaksgjennomforinger));
    }
  ),

  rest.get<any, any, Tiltaksgjennomforing[]>(
    "*/api/v1/internal/tiltaksgjennomforinger/sok",
    (req, res, ctx) => {
      const tiltaksnummer = req.url.searchParams.get("tiltaksnummer");

      if (!tiltaksnummer) {
        throw new Error("Tiltaksnummer er ikke satt som query-param");
      }

      const gjennomforing = mockTiltaksgjennomforinger.data.filter((tg) =>
        tg.tiltaksnummer.toString().includes(tiltaksnummer)
      );

      return res(ctx.status(200), ctx.json(gjennomforing));
    }
  ),

  rest.get<any, any, PaginertTiltaksgjennomforing>(
    "*/api/v1/internal/tiltaksgjennomforinger/mine",
    (req, res, ctx) => {
      const { tiltaksgjennomforinger } =
        mockTiltaksgjennomforingerKobletTilAnsatt[0] ?? {
          tiltaksgjennomforinger: [],
        };
      const gjennomforinger = mockTiltaksgjennomforinger.data.filter((gj) =>
        tiltaksgjennomforinger.includes(gj.id)
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
        })
      );
    }
  ),

  rest.get<any, { id: string }, Tiltaksgjennomforing | undefined>(
    "*/api/v1/internal/tiltaksgjennomforinger/:id",
    (req, res, ctx) => {
      const { id } = req.params;

      const gjennomforing = mockTiltaksgjennomforinger.data.find(
        (gj) => gj.id === id
      );
      if (!gjennomforing) {
        return res(ctx.status(404), ctx.json(undefined));
      }

      return res(ctx.status(200), ctx.json(gjennomforing));
    }
  ),

  rest.get<any, { id: string }, PaginertTiltaksgjennomforing | undefined>(
    "*/api/v1/internal/tiltaksgjennomforinger/tiltakstype/:id",
    (req, res, ctx) => {
      const { id } = req.params as { id: string };

      const gjennomforinger = mockTiltaksgjennomforinger.data.filter(
        (gj) => gj.tiltakstype.id === id
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
        })
      );
    }
  ),

  rest.get<any, { tiltakskode: string }, PaginertTiltaksgjennomforing>(
    "*/api/v1/internal/tiltaksgjennomforinger/tiltakskode/:tiltakskode",
    (req, res, ctx) => {
      const { tiltakskode } = req.params;
      const gjennomforinger = mockTiltaksgjennomforinger.data.filter(
        (gj) => gj.tiltakstype.arenaKode === tiltakskode
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
        })
      );
    }
  ),

  rest.get<any, { enhet: string }, PaginertTiltaksgjennomforing>(
    "*/api/v1/internal/tiltaksgjennomforinger/enhet/:enhet",
    (req, res, ctx) => {
      const { enhet } = req.params;
      const gjennomforinger = mockTiltaksgjennomforinger.data.filter(
        (gj) => gj.enhet === enhet
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
        })
      );
    }
  ),

  rest.get<any, any, Ansatt>("*/api/v1/internal/ansatt/me", (req, res, ctx) => {
    const rolleValgt =
      JSON.parse(window.localStorage.getItem("mr-admin-rolle")!!)?.toString() ??
      "TILTAKSANSVARLIG";

    return res(
      ctx.status(200),
      ctx.json(
        rolleValgt === "TILTAKSANSVARLIG"
          ? mockTiltaksansvarlig
          : mockFagansvarlig
      )
    );
  }),
];
