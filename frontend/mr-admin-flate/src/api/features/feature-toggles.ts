import { useQuery } from "@tanstack/react-query";
import { QueryKeys } from "../QueryKeys";
import { mulighetsrommetClient } from "../clients";
import { headers } from "../headers";

export const VIS_NOKKELTALL_ADMIN_FLATE =
  "mulighetsrommet.admin-flate-vis-nokkeltall";
export const OPPRETT_AVTALE_ADMIN_FLATE =
  "mulighetsrommet.admin-flate-opprett-avtale";
export const REDIGER_AVTALE_ADMIN_FLATE =
  "mulighetsrommet.admin-flate-rediger-avtale";
export const OPPRETT_TILTAKSGJENNOMFORING_ADMIN_FLATE =
  "mulighetsrommet.admin-flate-opprett-tiltaksgjennomforing";
export const SLETTE_AVTALE = "mulighetsrommet.admin-flate-slett-avtale";
export const REDIGER_TILTAKSGJENNOMFORING_ADMIN_FLATE =
  "mulighetsrommet.admin-flate-rediger-tiltaksgjennomforing";
export const SLETT_TILTAKSGJENNOMFORING_ADMIN_FLATE =
  "mulighetsrommet.admin-flate-slett-tiltaksgjennomforing";
export const VIS_DELTAKERLISTE_KOMET =
  "mulighetsrommet.admin-flate-vis-deltakerliste-fra-komet";
export const TEST_SINDRE = "test-sindre";

export const ALL_TOGGLES = [
  TEST_SINDRE,
  VIS_NOKKELTALL_ADMIN_FLATE,
  OPPRETT_AVTALE_ADMIN_FLATE,
  REDIGER_AVTALE_ADMIN_FLATE,
  OPPRETT_TILTAKSGJENNOMFORING_ADMIN_FLATE,
  SLETTE_AVTALE,
  REDIGER_TILTAKSGJENNOMFORING_ADMIN_FLATE,
  SLETT_TILTAKSGJENNOMFORING_ADMIN_FLATE,
  VIS_DELTAKERLISTE_KOMET,
] as const;

export type Features = Record<(typeof ALL_TOGGLES)[number], boolean>;

export const initialFeatures: Features = {
  "test-sindre": false,
  "mulighetsrommet.admin-flate-vis-nokkeltall": false,
  "mulighetsrommet.admin-flate-opprett-avtale": false,
  "mulighetsrommet.admin-flate-rediger-avtale": false,
  "mulighetsrommet.admin-flate-opprett-tiltaksgjennomforing": false,
  "mulighetsrommet.admin-flate-slett-avtale": false,
  "mulighetsrommet.admin-flate-slett-tiltaksgjennomforing": false,
  "mulighetsrommet.admin-flate-rediger-tiltaksgjennomforing": false,
  "mulighetsrommet.admin-flate-vis-deltakerliste-fra-komet": false,
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
 */
export const useFeatureToggle = (feature: keyof Features) => {
  return useQuery<boolean>(
    QueryKeys.features(feature),
    () => mulighetsrommetClient.features.getFeatureToggle({ feature }),
    { initialData: false },
  );
};
