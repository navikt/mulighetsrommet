import { headers } from './utils';
import { useQuery } from 'react-query';

export const ALERT_INFO = 'mulighetsrommet.alert-info';
export const FAKE_DOOR = 'mulighetsrommet.fake-door';

export const ALL_TOGGLES = [ALERT_INFO, FAKE_DOOR];

export interface Features {
  [ALERT_INFO]: boolean;
  [FAKE_DOOR]: boolean;
}

export const initialFeatures: Features = {
  [ALERT_INFO]: true,
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
