import { rest } from "msw";
import { mockFeatures } from "./api/features";
export const eksterneSystemerHandlers = [
  rest.get("*/api/feature", (req, res, ctx) => {
    return res(ctx.status(200), ctx.json(mockFeatures));
  }),
];
