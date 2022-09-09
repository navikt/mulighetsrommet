import {
  Features,
  ENABLE_ARBEIDSFLATE,
  FEEDBACK,
  DELING_MED_BRUKER,
  VIS_HISTORIKK,
} from '../../core/api/feature-toggles';

export const mockFeatures: Features = {
  [ENABLE_ARBEIDSFLATE]: false,
  [FEEDBACK]: true,
  [DELING_MED_BRUKER]: true,
  [VIS_HISTORIKK]: true,
};
