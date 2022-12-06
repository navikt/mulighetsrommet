// src/mocks/handlers.js
import { rest } from "msw";
import { mockAnsatt } from "./fixtures/mock_ansatt";
import { mockTiltaksgjennomforinger } from "./fixtures/mock_tiltaksgjennomforinger";
import { mockTiltakstyper } from "./fixtures/mock_tiltakstyper";

export const apiHandlers = [
  rest.get("*/api/v1/tiltakstyper", (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(mockTiltakstyper));
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

  rest.get("*/api/v1/ansatt/me", (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(mockAnsatt));
  }),
];
