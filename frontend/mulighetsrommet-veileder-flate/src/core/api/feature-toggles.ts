import { useQuery } from 'react-query';
import { headers } from './headers';

export const ENABLE_ARBEIDSFLATE = 'mulighetsrommet.enable-arbeidsflate';

export const ALL_TOGGLES = [ENABLE_ARBEIDSFLATE];

export interface Features {
  [ENABLE_ARBEIDSFLATE]: boolean;
}

export const initialFeatures: Features = {
  [ENABLE_ARBEIDSFLATE]: false,
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
