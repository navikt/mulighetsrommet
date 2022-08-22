import { headers } from './headers';
import { useQuery } from 'react-query';

export const FAKE_DOOR = 'mulighetsrommet.fake-door';
export const FEEDBACK = 'mulighetsrommet.feedback';

export const ALL_TOGGLES = [FAKE_DOOR, FEEDBACK];

//NB! Husk å legg til ny feature-toggle i features.ts også (mock)

export interface Features {
  [FAKE_DOOR]: boolean;
  [FEEDBACK]: boolean;
}

export const initialFeatures: Features = {
  [FAKE_DOOR]: false,
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
