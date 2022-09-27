import {
  ENABLE_ARBEIDSFLATE,
  Features,
  FEEDBACK,
  LAGRE_DEL_TILTAK_MED_BRUKER,
  VIS_HISTORIKK,
} from '../../core/api/feature-toggles';

export const mockFeatures: Features = {
  [ENABLE_ARBEIDSFLATE]: true,
  [FEEDBACK]: true,
  [VIS_HISTORIKK]: true,
  [LAGRE_DEL_TILTAK_MED_BRUKER]: true,
};
