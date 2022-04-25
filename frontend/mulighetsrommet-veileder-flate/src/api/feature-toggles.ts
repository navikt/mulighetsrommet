import { headers } from './utils';
import { useQuery } from 'react-query';

export const FAKE_DOOR = 'mulighetsrommet.fake-door';

export const ALL_TOGGLES = [FAKE_DOOR];

export interface Features {
  [FAKE_DOOR]: boolean;
}

export const initialFeatures: Features = {
  [FAKE_DOOR]: false,
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
