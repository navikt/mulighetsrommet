import { Features, ENABLE_ARBEIDSFLATE, FEEDBACK, DELING_MED_BRUKER } from "../../core/api/feature-toggles";

export const mockFeatures: Features = {
  [ENABLE_ARBEIDSFLATE]: true,
  [FEEDBACK]: true,
  [DELING_MED_BRUKER]: true,
};
