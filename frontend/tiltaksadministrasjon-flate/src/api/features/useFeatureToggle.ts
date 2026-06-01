import { useApiQuery } from "@mr/frontend-common";
import {
  FeatureToggle,
  FeatureToggleService,
  Tiltakskode,
} from "@tiltaksadministrasjon/api-client";

export type Features = Record<FeatureToggle, boolean>;

/**
 * Hook for å bruke en spesifikk feature toggle for å skjule eller vise funksjonalitet
 * @param feature Navn på feature-toggle du vil bruke
 * @param tiltakskoder Input til "by-tiltakskode"-stategi
 * @returns true hvis toggle er skrudd på, eller false hvis ikke
 */
export function useFeatureToggle(feature: FeatureToggle, tiltakskoder: Tiltakskode[] = []) {
  return useApiQuery({
    queryKey: ["feature", feature, tiltakskoder],
    queryFn: () => FeatureToggleService.getFeatureToggle({ query: { feature, tiltakskoder } }),
    throwOnError: false,
  });
}
