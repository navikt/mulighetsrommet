// src/mocks/handlers.js
import { rest } from "msw";
import { mockFagansvarlig, mockTiltaksansvarlig } from "./fixtures/mock_ansatt";
import { mockTiltaksgjennomforinger } from "./fixtures/mock_tiltaksgjennomforinger";
import { mockTiltaksgjennomforingerKobletTilAnsatt } from "./fixtures/mock_tiltaksgjennomforinger_koblet_til_ansatt";
import { mockTiltakstyper } from "./fixtures/mock_tiltakstyper";

export const apiHandlers = [
  rest.get("*/api/v1/internal/tiltakstyper", (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(mockTiltakstyper));
  }),

  rest.get("*/api/v1/internal/tiltakstyper/:id", (req, res, ctx) => {
    const { id } = req.params as { id: string };
    return res(
      ctx.status(200),

      ctx.json(mockTiltakstyper.data.find((gj) => gj.id === id))
    );
  }),

  rest.get("*/api/v1/internal/tiltaksgjennomforinger", (req, res, ctx) => {
    return res(
      ctx.status(200),
      ctx.delay(350),
      ctx.json(mockTiltaksgjennomforinger)
    );
  }),

  rest.get("*/api/v1/internal/tiltaksgjennomforinger/sok", (req, res, ctx) => {
    const tiltaksnummer = req.url.searchParams.get("tiltaksnummer");

    if (!tiltaksnummer) {
      throw new Error("Tiltaksnummer er ikke satt som query-param");
    }

    const gjennomforing = mockTiltaksgjennomforinger.data.filter((tg) =>
      tg.tiltaksnummer.toString().includes(tiltaksnummer)
    );

    return res(ctx.status(200), ctx.delay(350), ctx.json(gjennomforing));
  }),

  rest.get("*/api/v1/internal/tiltaksgjennomforinger/mine", (req, res, ctx) => {
    const { tiltaksgjennomforinger } =
      mockTiltaksgjennomforingerKobletTilAnsatt[0] ?? {
        tiltaksgjennomforinger: [],
      };
    const gjennomforinger = mockTiltaksgjennomforinger.data.filter((gj) =>
      tiltaksgjennomforinger.includes(gj.id)
    );
    return res(
      ctx.status(200),
      ctx.delay(350),
      ctx.json({
        pagination: {
          totalCount: gjennomforinger.length,
          currentPage: 1,
          pageSize: 50,
        },
        data: gjennomforinger,
      })
    );
  }),

  rest.get("*/api/v1/internal/tiltaksgjennomforinger/:id", (req, res, ctx) => {
    const { id } = req.params as { id: string };

    const gjennomforing = mockTiltaksgjennomforinger.data.find(
      (gj) => gj.id === id
    );
    if (!gjennomforing) {
      return res(ctx.status(404), ctx.json(undefined));
    }

    return res(ctx.status(200), ctx.delay(350), ctx.json(gjennomforing));
  }),

  rest.get(
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
        ctx.delay(350),
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

  rest.get(
    "*/api/v1/internal/tiltaksgjennomforinger/tiltakskode/:tiltakskode",
    (req, res, ctx) => {
      const { tiltakskode } = req.params as { tiltakskode: string };
      const gjennomforinger = mockTiltaksgjennomforinger.data.filter(
        (gj) => gj.tiltakstype.arenaKode === tiltakskode
      );
      return res(
        ctx.status(200),
        ctx.delay(450),
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

  rest.get(
    "*/api/v1/internal/tiltaksgjennomforinger/enhet/:enhet",
    (req, res, ctx) => {
      const { enhet } = req.params as { enhet: string };
      const gjennomforinger = mockTiltaksgjennomforinger.data.filter(
        (gj) => gj.enhet === enhet
      );
      return res(
        ctx.status(200),
        ctx.delay(350),
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

  rest.get("*/api/v1/internal/ansatt/me", (req, res, ctx) => {
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
