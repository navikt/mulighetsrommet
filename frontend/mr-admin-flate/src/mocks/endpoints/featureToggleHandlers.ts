import { http, HttpResponse } from "msw";
import { Features } from "@/api/features/useFeatureToggle";
import { Toggles } from "@mr/api-client-v2";

const mockFeatures: Features = {
  [Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_TILSAGN]: true,
  [Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_OKONOMI]: true,
  [Toggles.ARRANGORFLATE_UTBETALING_OPPRETT_UTBETALING_KNAPP]: true,
};

export const featureToggleHandlers = [
  http.get("*/api/v1/intern/features", ({ request }) => {
    const url = new URL(request.url);
    const feature = url.searchParams.get("feature") as keyof Features;
    if (!feature) {
      throw Error("Feature er ikke satt");
    }
    return HttpResponse.json(mockFeatures[feature]);
  }),
];
