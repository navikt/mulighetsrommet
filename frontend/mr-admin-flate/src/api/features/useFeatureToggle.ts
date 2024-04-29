import { useQuery } from "@tanstack/react-query";
import { Toggles } from "mulighetsrommet-api-client";
import { QueryKeys } from "@/api/QueryKeys";
import { mulighetsrommetClient } from "@/api/client";

export type Features = Record<Toggles, boolean>;

/**
 * Hook for å bruke en spesifikk feature toggle for å skjule eller vise funksjonalitet
 * @param feature Navn på feature-toggle du vil bruke
 * @returns true hvis toggle er skrudd på, eller false hvis ikke
 */
export const useFeatureToggle = (feature: Toggles) => {
  return useQuery({
    queryKey: QueryKeys.features(feature),
    queryFn: () => mulighetsrommetClient.features.getFeatureToggle({ feature }),
    throwOnError: false,
  });
};
