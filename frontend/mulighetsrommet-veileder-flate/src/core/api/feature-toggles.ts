import { Toggles } from 'mulighetsrommet-api-client';
import { useQuery } from 'react-query';
import { mulighetsrommetClient } from './clients';
import { QueryKeys } from './query-keys';

export const ALL_TOGGLES = [...Object.values(Toggles)];

export type Features = Record<Toggles, boolean>;

/**
 * Hook for å bruke en spesifikk feature toggle for å skjule eller vise funksjonalitet
 * @param feature Navn på feature-toggle du vil bruke
 * @returns true hvis toggle er skrudd på, eller false hvis ikke
 */
export const useFeatureToggle = (feature: Toggles) => {
  return useQuery<boolean>(QueryKeys.features(feature), () =>
    mulighetsrommetClient.features.getFeatureToggle({ feature })
  );
};
