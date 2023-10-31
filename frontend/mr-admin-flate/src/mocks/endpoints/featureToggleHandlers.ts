import { HttpResponse, http } from "msw";
import { mockFeatures } from "../api/features";
import invariant from "tiny-invariant";
import { Features } from "../../api/features/feature-toggles";

export const featureToggleHandlers = [
  http.get("*/api/v1/internal/features", ({ request }) => {
    const url = new URL(request.url);
    const feature = url.searchParams.get("feature") as keyof Features;
    invariant(feature, "Feature er ikke satt");
    return HttpResponse.json(mockFeatures[feature]);
  }),
];
