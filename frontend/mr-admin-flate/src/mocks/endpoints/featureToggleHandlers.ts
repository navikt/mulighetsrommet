import { http, HttpResponse } from "msw";
import { Features } from "@/api/features/useFeatureToggle";
import { Toggles } from "@mr/api-client";

const mockFeatures: Features = {
  [Toggles.MULIGHETSROMMET_VEILEDERFLATE_VIS_TILBAKEMELDING]: true,
  [Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_DELTAKER]: true,
  [Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_OKONOMI]: true,
  [Toggles.MULIGHETSROMMET_TILTAKSTYPE_PAMELDING_INFO_KOMET]: true,
  [Toggles.MULIGHETSROMMET_ADMIN_FLATE_ARBEIDSBENK]: true,
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
