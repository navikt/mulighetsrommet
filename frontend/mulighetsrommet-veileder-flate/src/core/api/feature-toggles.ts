import { useQuery } from 'react-query';
import { headers } from './headers';

export const ENABLE_ARBEIDSFLATE = 'mulighetsrommet.enable-arbeidsflate';
export const VIS_INNSIKTSFANE = 'mulighetsrommet.vis-innsiktsfane';

export const ALL_TOGGLES = [ENABLE_ARBEIDSFLATE, VIS_INNSIKTSFANE];

export interface Features {
  [ENABLE_ARBEIDSFLATE]: boolean;
  [VIS_INNSIKTSFANE]: boolean;
}

export const initialFeatures: Features = {
  [ENABLE_ARBEIDSFLATE]: false,
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
