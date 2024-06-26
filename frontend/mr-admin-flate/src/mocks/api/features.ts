import { Features } from "@/api/features/useFeatureToggle";
import { Toggles } from "mulighetsrommet-api-client";

export const mockFeatures: Features = {
  [Toggles.MULIGHETSROMMET_VEILEDERFLATE_VIS_DELTAKER_REGISTRERING]: false,
  [Toggles.MULIGHETSROMMET_VEILEDERFLATE_LANDINGSSIDE]: true,
  [Toggles.MULIGHETSROMMET_VEILEDERFLATE_ARENA_OPPSKRIFTER]: true,
  [Toggles.MULIGHETSROMMET_ADMIN_FLATE_ENABLE_DEBUGGER]: true,
  [Toggles.MULIGHETSROMMET_ADMIN_FLATE_ENABLE_GRUPPE_AMO_KATEGORIER]: true,
  [Toggles.MULIGHETSROMMET_VEILEDERFLATE_ENABLE_V2HISTORIKK]: true,
};
