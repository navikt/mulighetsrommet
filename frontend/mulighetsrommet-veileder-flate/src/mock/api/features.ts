import { ENABLE_ARBEIDSFLATE, Features, VIS_INNSIKTSFANE, VIS_JOYRIDE } from '../../core/api/feature-toggles';

export const mockFeatures: Features = {
  [ENABLE_ARBEIDSFLATE]: true,
  [VIS_INNSIKTSFANE]: false,
  [VIS_JOYRIDE]: true,
};
