import { headers } from "../headers";
import { useQuery } from "@tanstack/react-query";

export const ENABLE_ADMIN_FLATE = "mulighetsrommet.enable-admin-flate";

export const ENABLE_OPPRETT_GJENNOMFORING =
  "mulighetsrommet.enable-opprett-gjennomforing";

export const ALL_TOGGLES = [
  ENABLE_ADMIN_FLATE,
  ENABLE_OPPRETT_GJENNOMFORING,
] as const;

export type Features = Record<typeof ALL_TOGGLES[number], boolean>;

export const initialFeatures: Features = {
  "mulighetsrommet.enable-admin-flate": false,
  "mulighetsrommet.enable-opprett-gjennomforing": false,
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
