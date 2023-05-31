import { headers } from "../headers";
import { useQuery } from "@tanstack/react-query";

export const ENABLE_ADMIN_FLATE = "mulighetsrommet.enable-admin-flate";
export const VIS_NOKKELTALL_ADMIN_FLATE =
  "mulighetsrommet.admin-flate-vis-nokkeltall";
export const OPPRETT_AVTALE_ADMIN_FLATE =
  "mulighetsrommet.admin-flate-opprett-avtale";
export const REDIGER_AVTALE_ADMIN_FLATE =
  "mulighetsrommet.admin-flate-rediger-avtale";
export const OPPRETT_TILTAKSGJENNOMFORING_ADMIN_FLATE =
  "mulighetsrommet.admin-flate-opprett-tiltaksgjennomforing";
export const SE_NOTIFIKASJONER_ADMIN_FLATE =
  "mulighetsrommet.admin-flate-se-notifikasjoner";
export const LAGRE_DATA_FRA_ADMIN_FLATE =
  "mulighetsrommet.admin-flate-lagre-data-fra-admin-flate";

export const ALL_TOGGLES = [
  ENABLE_ADMIN_FLATE,
  VIS_NOKKELTALL_ADMIN_FLATE,
  OPPRETT_AVTALE_ADMIN_FLATE,
  REDIGER_AVTALE_ADMIN_FLATE,
  OPPRETT_TILTAKSGJENNOMFORING_ADMIN_FLATE,
  SE_NOTIFIKASJONER_ADMIN_FLATE,
  LAGRE_DATA_FRA_ADMIN_FLATE,
] as const;

export type Features = Record<(typeof ALL_TOGGLES)[number], boolean>;

export const initialFeatures: Features = {
  "mulighetsrommet.enable-admin-flate": false,
  "mulighetsrommet.admin-flate-vis-nokkeltall": false,
  "mulighetsrommet.admin-flate-opprett-avtale": false,
  "mulighetsrommet.admin-flate-rediger-avtale": false,
  "mulighetsrommet.admin-flate-opprett-tiltaksgjennomforing": false,
  "mulighetsrommet.admin-flate-se-notifikasjoner": false,
  "mulighetsrommet.admin-flate-lagre-data-fra-admin-flate": false,
};

const toggles = ALL_TOGGLES.map((element) => "feature=" + element).join("&");
export const fetchConfig = {
  headers,
};

export const useFeatureToggles = () => {
  return useQuery<Features>(["features"], () =>
    fetch(`/unleash/api/feature?${toggles}`, fetchConfig).then((Response) => {
      return Response.ok ? Response.json() : initialFeatures;
    })
  );
};
