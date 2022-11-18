// src/mocks/handlers.js
import { rest } from "msw";
import { mockFeatures } from "./api/features";

export const handlers = [
  rest.get("*/api/feature", (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(mockFeatures));
  }),
];
