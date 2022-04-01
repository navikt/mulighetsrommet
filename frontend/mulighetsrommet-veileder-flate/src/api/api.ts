import { ALL_TOGGLES, Features } from './features';
import useFetch from '@nutgaard/use-fetch';
import { headers } from './utils';

const toggles = ALL_TOGGLES.map(element => 'feature=' + element).join('&');

export const useFetchFeatureToggle = () =>
  useFetch<Features>(`/veilarbpersonflatefs/api/feature?${toggles}`, { headers });
