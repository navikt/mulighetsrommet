import { ENABLE_ARBEIDSFLATE, Features, FEEDBACK, VIS_HISTORIKK } from '../../core/api/feature-toggles';

export const mockFeatures: Features = {
  [ENABLE_ARBEIDSFLATE]: true,
  [FEEDBACK]: true,
  [VIS_HISTORIKK]: true,
};
