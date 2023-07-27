import { rest } from "msw";
import { mockFeatures } from "../api/features";
import invariant from "tiny-invariant";
import { Features } from "../../api/features/feature-toggles";

export const featureToggleHandlers = [
  rest.get("*/api/v1/internal/features", (req, res, ctx) => {
    const feature = req.url.searchParams.get("feature") as keyof Features;
    invariant(feature, "Feature er ikke satt");
    return res(ctx.status(200), ctx.json(mockFeatures[feature]));
  }),
];
