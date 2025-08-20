import { http, HttpResponse } from "msw";
import { Features } from "@/api/features/useFeatureToggle";
import { Toggles } from "@mr/api-client-v2";

const mockFeatures: Features = {
  [Toggles.MULIGHETSROMMET_MIGRERING_OKONOMI_AVBRYT_UTBETALING]: true,
  [Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_TILSAGN]: true,
  [Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_UTBETALING]: true,
  [Toggles.ARRANGORFLATE_OPPRETT_UTBETEALING_INVESTERINGER]: true,
  [Toggles.ARRANGORFLATE_OPPRETT_UTBETALING_ANNEN_AVTALT_PPRIS]: true,
};

export const featureToggleHandlers = [
  http.get("*/api/v1/intern/features", ({ request }) => {
    const url = new URL(request.url);
    const feature = url.searchParams.get("feature") as keyof Features;
    return HttpResponse.json(mockFeatures[feature]);
  }),
];
