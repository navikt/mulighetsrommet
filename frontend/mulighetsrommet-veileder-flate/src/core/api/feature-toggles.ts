import { headers } from './headers';
import { useQuery } from 'react-query';

export const ENABLE_ARBEIDSFLATE = 'mulighetsrommet.enable-arbeidsflate';
export const FEEDBACK = 'mulighetsrommet.feedback';

export const ALL_TOGGLES = [ENABLE_ARBEIDSFLATE, FEEDBACK];

export interface Features {
  [ENABLE_ARBEIDSFLATE]: boolean;
  [FEEDBACK]: boolean;

}

export const initialFeatures: Features = {
  [ENABLE_ARBEIDSFLATE]: false,
  [FEEDBACK]: true,

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
