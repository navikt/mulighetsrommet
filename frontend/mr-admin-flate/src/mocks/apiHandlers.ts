// src/mocks/handlers.js
import { rest } from "msw";
import { mock_ansatt } from "./fixtures/mock_ansatt";
import { mock_tiltaksgjennomforinger } from "./fixtures/mock_tiltaksgjennomforinger";
import { mock_tiltakstyper } from "./fixtures/mock_tiltakstyper";

export const apiHandlers = [
  rest.get("*/api/v1/tiltakstyper", (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(mock_tiltakstyper));
  }),
  rest.get("*/api/v1/tiltaksgjennomforinger", (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(mock_tiltaksgjennomforinger));
  }),

  rest.get("*/api/v1/ansatt", (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(mock_ansatt));
  }),
];
