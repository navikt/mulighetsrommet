import {
  ENABLE_ARBEIDSFLATE,
  Features,
  FEEDBACK,
  LAGRE_DEL_TILTAK_MED_BRUKER,
  VIS_HISTORIKK,
  VIS_INNSIKTSFANE,
} from '../../core/api/feature-toggles';

export const mockFeatures: Features = {
  [ENABLE_ARBEIDSFLATE]: true,
  [FEEDBACK]: true,
  [VIS_HISTORIKK]: true,
  [LAGRE_DEL_TILTAK_MED_BRUKER]: true,
  [VIS_INNSIKTSFANE]: false,
};
