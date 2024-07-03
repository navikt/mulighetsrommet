import { Features } from "@/api/feature-toggles";
import { Toggles } from "mulighetsrommet-api-client";

export const mockFeatures: Features = {
  [Toggles.MULIGHETSROMMET_VEILEDERFLATE_VIS_DELTAKER_REGISTRERING]: true,
  [Toggles.MULIGHETSROMMET_VEILEDERFLATE_LANDINGSSIDE]: false,
  [Toggles.MULIGHETSROMMET_VEILEDERFLATE_ARENA_OPPSKRIFTER]: true,
  [Toggles.MULIGHETSROMMET_ADMIN_FLATE_ENABLE_DEBUGGER]: false,
  [Toggles.MULIGHETSROMMET_ADMIN_FLATE_ENABLE_GRUPPE_AMO_KATEGORIER]: true,
  [Toggles.MULIGHETSROMMET_VEILEDERFLATE_ENABLE_V2HISTORIKK]: true,
  [Toggles.MULIGHETSROMMET_ADMIN_FLATE_REGISTRERE_OPSJONSMODELL]: true,
};
