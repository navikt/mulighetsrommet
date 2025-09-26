import { http, HttpResponse } from "msw";
import { Features } from "@/api/features/useFeatureToggle";
import { FeatureToggle } from "@tiltaksadministrasjon/api-client";

const mockFeatures: Features = {
  [FeatureToggle.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_TILSAGN]: true,
  [FeatureToggle.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_UTBETALING]: true,
  [FeatureToggle.MULIGHETSROMMET_PRISMODELL_HELE_UKER]: true,
  [FeatureToggle.ARRANGORFLATE_OPPRETT_UTBETALING_ANNEN_AVTALT_PPRIS]: true,
};

export const featureToggleHandlers = [
  http.get("*/api/tiltaksadministrasjon/features", ({ request }) => {
    const url = new URL(request.url);
    const feature = url.searchParams.get("feature") as keyof Features;
    return HttpResponse.json(mockFeatures[feature]);
  }),
];
