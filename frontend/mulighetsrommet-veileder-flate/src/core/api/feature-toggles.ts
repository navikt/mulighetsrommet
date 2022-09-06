import { headers } from './headers';
import { useQuery } from 'react-query';

export const ENABLE_ARBEIDSFLATE = 'mulighetsrommet.enable-arbeidsflate';
export const FEEDBACK = 'mulighetsrommet.feedback';
export const DELING_MED_BRUKER = 'mulighetsrommet.deling-med-bruker';
export const VIS_HISTORIKK = 'mulighetsrommet.vis-historikk';

export const ALL_TOGGLES = [ENABLE_ARBEIDSFLATE, FEEDBACK, DELING_MED_BRUKER, VIS_HISTORIKK];

export interface Features {
  [ENABLE_ARBEIDSFLATE]: boolean;
  [FEEDBACK]: boolean;
  [DELING_MED_BRUKER]: boolean;
  [VIS_HISTORIKK]: boolean;
}

export const initialFeatures: Features = {
  [ENABLE_ARBEIDSFLATE]: false,
  [DELING_MED_BRUKER]: false,
  [FEEDBACK]: false,
  [VIS_HISTORIKK]: false,
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
