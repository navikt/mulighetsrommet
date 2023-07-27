import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";
import { headers } from "../headers";
import { Toggles } from "mulighetsrommet-api-client";

export const ALL_TOGGLES = [...Object.values(Toggles)] as const;

export type Features = Record<Toggles, boolean>;

export const initialFeatures: Features = {
  "mulighetsrommet.admin-flate-opprett-avtale": false,
  "mulighetsrommet.admin-flate-rediger-avtale": false,
  "mulighetsrommet.admin-flate-opprett-tiltaksgjennomforing": false,
  "mulighetsrommet.admin-flate-slett-avtale": false,
  "mulighetsrommet.admin-flate-slett-tiltaksgjennomforing": false,
  "mulighetsrommet.admin-flate-rediger-tiltaksgjennomforing": false,
  "mulighetsrommet.admin-flate-vis-deltakerliste-fra-komet": false,
  "mulighetsrommet.enable-arbeidsflate": true,
};

const toggles = ALL_TOGGLES.map((element) => "feature=" + element).join("&");
export const fetchConfig = {
  headers,
};

export const useFeatureToggles = () => {
  return useQuery<Features>(["features"], () =>
    fetch(`/unleash/api/feature?${toggles}`, fetchConfig).then((Response) => {
      return Response.ok ? Response.json() : initialFeatures;
    }),
  );
};

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
