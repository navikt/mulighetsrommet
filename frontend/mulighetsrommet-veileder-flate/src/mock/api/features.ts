import {
  ENABLE_ARBEIDSFLATE,
  Features,
  FEEDBACK,
  VIS_HISTORIKK,
  VIS_INNSIKTSFANE,
  VIS_TILGJENGELIGHETSSTATUS,
} from '../../core/api/feature-toggles';

export const mockFeatures: Features = {
  [ENABLE_ARBEIDSFLATE]: true,
  [FEEDBACK]: true,
  [VIS_HISTORIKK]: true,
  [VIS_INNSIKTSFANE]: false,
  [VIS_TILGJENGELIGHETSSTATUS]: false,
};
