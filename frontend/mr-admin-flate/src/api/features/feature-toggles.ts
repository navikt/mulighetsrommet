import { useQuery } from "@tanstack/react-query";
import { Toggles } from "mulighetsrommet-api-client";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";

export const ALL_TOGGLES = [...Object.values(Toggles)] as const;

export type Features = Record<Toggles, boolean>;

/**
 * Hook for å bruke en spesifikk feature toggle for å skjule eller vise funksjonalitet
 * @param feature Navn på feature-toggle du vil bruke
 * @returns true hvis toggle er skrudd på, eller false hvis ikke
 *
 * @param initialValue Overstyr initiell verdi
 * @returns Verdi for initialValue før nettverkskall er ferdig
 */
export const useFeatureToggle = (feature: Toggles) => {
  return useQuery<boolean>(QueryKeys.features(feature), () =>
    mulighetsrommetClient.features.getFeatureToggle({ feature }),
  );
};
