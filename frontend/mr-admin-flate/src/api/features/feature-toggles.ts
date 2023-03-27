import { headers } from "../headers";
import { useQuery } from "@tanstack/react-query";

export const ENABLE_ADMIN_FLATE = "mulighetsrommet.enable-admin-flate";
export const VIS_NOKKELTALL_ADMIN_FLATE =
  "mulighetsrommet.admin-flate-vis-nokkeltall";
export const OPPRETT_AVTALE_ADMIN_FLATE =
  "mulighetsrommet.admin-flate-opprett-avtale";

export const ALL_TOGGLES = [
  ENABLE_ADMIN_FLATE,
  VIS_NOKKELTALL_ADMIN_FLATE,
  OPPRETT_AVTALE_ADMIN_FLATE,
] as const;

export type Features = Record<(typeof ALL_TOGGLES)[number], boolean>;

export const initialFeatures: Features = {
  "mulighetsrommet.enable-admin-flate": false,
  "mulighetsrommet.admin-flate-vis-nokkeltall": false,
  "mulighetsrommet.admin-flate-opprett-avtale": false,
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
