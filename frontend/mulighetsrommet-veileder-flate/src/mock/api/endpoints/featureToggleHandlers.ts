import { http, HttpResponse } from "msw";
import { Features } from "@/api/feature-toggles";
import { FeatureToggle } from "@api-client";

const mockFeatures: Features = {
  [FeatureToggle.AVBRYT_UTBETALING]: true,
  [FeatureToggle.MIGRERING_TILSAGN]: true,
  [FeatureToggle.MIGRERING_UTBETALING]: true,
  [FeatureToggle.ARRANGORFLATE_OPPRETT_UTBETEALING_INVESTERINGER]: true,
  [FeatureToggle.ARRANGORFLATE_OPPRETT_UTBETALING_ANNEN_AVTALT_PPRIS]: true,
};

export const featureToggleHandlers = [
  http.get("*/api/veilederflate/features", ({ request }) => {
    const url = new URL(request.url);
    const feature = url.searchParams.get("feature") as keyof Features;
    if (!feature) {
      throw Error("Feature er ikke satt");
    }
    return HttpResponse.json(mockFeatures[feature]);
  }),
];
