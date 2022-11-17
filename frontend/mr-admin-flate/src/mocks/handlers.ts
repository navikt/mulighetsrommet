// src/mocks/handlers.js
import { rest } from "msw";

export const handlers = [
  // Handles a POST /login request
  rest.post("/tiltakstyper", (req, res, ctx) => {
    return res(ctx.status(200), ctx.json({ data: [] }));
  }),
];
