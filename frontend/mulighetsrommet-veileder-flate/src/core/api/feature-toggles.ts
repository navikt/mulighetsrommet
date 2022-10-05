import { headers } from './headers';
import { useQuery } from 'react-query';

export const ENABLE_ARBEIDSFLATE = 'mulighetsrommet.enable-arbeidsflate';
export const FEEDBACK = 'mulighetsrommet.feedback';
export const VIS_HISTORIKK = 'mulighetsrommet.vis-historikk';
export const LAGRE_DEL_TILTAK_MED_BRUKER = 'mulighetsrommet.lagre-del-tiltak-med-bruker';
export const VIS_INNSIKTSFANE = 'mulighetsrommet.vis-innsiktsfane';

export const ALL_TOGGLES = [
  ENABLE_ARBEIDSFLATE,
  FEEDBACK,
  VIS_HISTORIKK,
  LAGRE_DEL_TILTAK_MED_BRUKER,
  VIS_INNSIKTSFANE,
];

export interface Features {
  [ENABLE_ARBEIDSFLATE]: boolean;
  [FEEDBACK]: boolean;
  [VIS_HISTORIKK]: boolean;
  [LAGRE_DEL_TILTAK_MED_BRUKER]: boolean;
  [VIS_INNSIKTSFANE]: boolean;
}

export const initialFeatures: Features = {
  [ENABLE_ARBEIDSFLATE]: false,
  [FEEDBACK]: false,
  [VIS_HISTORIKK]: false,
  [LAGRE_DEL_TILTAK_MED_BRUKER]: true,
  [VIS_INNSIKTSFANE]: false,
};

const toggles = ALL_TOGGLES.map(element => 'feature=' + element).join('&');
export const fetchConfig = {
  headers,
};

export const useFeatureToggles = () => {
  return useQuery<Features>(['features'], () =>
    fetch(`/veilarbpersonflatefs/api/feature?${toggles}`, fetchConfig).then(Response => {
      return Response.ok ? Response.json() : initialFeatures;
    })
  );
};
