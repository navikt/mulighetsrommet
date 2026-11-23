import { http, HttpResponse } from "msw";
import { Features } from "@/api/features/useFeatureToggle";
import { FeatureToggle } from "@tiltaksadministrasjon/api-client";

const mockFeatures: Features = {
  [FeatureToggle.FAKE_TOGGLE]: true,
};

export const featureToggleHandlers = [
  http.get("*/api/tiltaksadministrasjon/features", ({ request }) => {
    const url = new URL(request.url);
    const feature = url.searchParams.get("feature") as keyof Features;
    return HttpResponse.json(mockFeatures[feature]);
  }),
];
