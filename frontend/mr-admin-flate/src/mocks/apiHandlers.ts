// src/mocks/handlers.js
import { rest } from "msw";
import { mockFagansvarlig, mockTiltaksansvarlig } from "./fixtures/mock_ansatt";
import { mockTiltaksgjennomforinger } from "./fixtures/mock_tiltaksgjennomforinger";
import { mockTiltakstyper } from "./fixtures/mock_tiltakstyper";

export const apiHandlers = [
  rest.get("*/api/v1/tiltakstyper", (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(mockTiltakstyper));
  }),

  rest.get("*/api/v1/tiltakstyper/:id", (req, res, ctx) => {
    const { id } = req.params as { id: string };
    return res(
      ctx.status(200),
      ctx.json(mockTiltakstyper.data.find((gj) => gj.id === id))
    );
  }),

  rest.get("*/api/v1/tiltaksgjennomforinger", (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(mockTiltaksgjennomforinger));
  }),

  rest.get("*/api/v1/tiltaksgjennomforinger/:id", (req, res, ctx) => {
    const { id } = req.params as { id: string };
    return res(
      ctx.status(200),
      ctx.json(mockTiltaksgjennomforinger.data.find((gj) => gj.id === id))
    );
  }),

  rest.get(
    "*/api/v1/tiltaksgjennomforinger/tiltakstypedata/:id",
    (req, res, ctx) => {
      const { id } = req.params as { id: string };
      return res(
        ctx.status(200),
        ctx.json(mockTiltaksgjennomforinger.data.find((gj) => gj.id === id))
      );
    }
  ),

  rest.get(
    "*/api/v1/tiltaksgjennomforinger/tiltakskode/:tiltakskode",
    (req, res, ctx) => {
      const { tiltakskode } = req.params as { tiltakskode: string };
      const gjennomforinger = mockTiltaksgjennomforinger.data.filter(
        (gj) => gj.tiltakskode === tiltakskode
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

  rest.get("*/api/v1/ansatt/me", (req, res, ctx) => {
    const rolleValgt =
      window.localStorage.getItem("valp-rolle-adminflate") ??
      "tiltaksansvarlig";

    return res(
      ctx.status(200),
      ctx.json(
        rolleValgt === "tiltaksansvarlig"
          ? mockTiltaksansvarlig
          : mockFagansvarlig
      )
    );
  }),
];
