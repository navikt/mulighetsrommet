import { ALL_TOGGLES, Features } from './features';
import useFetch from '@nutgaard/use-fetch';
import { APP_NAME } from '../utils/constants';

const headers = {
  headers: { 'Nav-Consumer-Id': APP_NAME },
};

const toggles = ALL_TOGGLES.map(element => 'feature=' + element).join('&');

export const useFetchFeatureToggle = () => useFetch<Features>(`/api/feature?${toggles}`, headers);
