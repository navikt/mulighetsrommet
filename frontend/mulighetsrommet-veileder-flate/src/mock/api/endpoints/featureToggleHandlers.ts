import { HttpResponse, http } from "msw";
import invariant from "tiny-invariant";
import { Features } from "@/api/feature-toggles";
import { mockFeatures } from "../features";

export const featureToggleHandlers = [
  http.get("*/api/v1/internal/features", ({ request }) => {
    const url = new URL(request.url);
    const feature = url.searchParams.get("feature") as keyof Features;
    invariant(feature, "Feature er ikke satt");
    return HttpResponse.json(mockFeatures[feature]);
  }),
];
