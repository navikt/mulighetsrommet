// src/mocks/handlers.js
import { rest } from "msw";
import { mockFeatures } from "./api/features";

export const handlers = [
  // Handles a POST /login request
  rest.get("*/api/feature", (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(mockFeatures));
  }),
];
