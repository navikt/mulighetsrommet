import { useQueryWrapper } from "@/hooks/useQueryWrapper";
import { FeatureToggleService, Tiltakskode, Toggles } from "@mr/api-client-v2";

export type Features = Record<Toggles, boolean>;

/**
 * Hook for å bruke en spesifikk feature toggle for å skjule eller vise funksjonalitet
 * @param feature Navn på feature-toggle du vil bruke
 * @param tiltakskoder Input til "by-tiltakskode"-stategi
 * @returns true hvis toggle er skrudd på, eller false hvis ikke
 */
export function useFeatureToggle(feature: Toggles, tiltakskoder: Tiltakskode[] = []) {
  return useQueryWrapper({
    queryKey: ["feature", feature, tiltakskoder],
    queryFn: () =>
      FeatureToggleService.getFeatureToggle<false>({ query: { feature, tiltakskoder } }),
    throwOnError: false,
  });
}
