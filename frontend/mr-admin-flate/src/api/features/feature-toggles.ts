import { headers } from "../headers";
import { useQuery } from "@tanstack/react-query";

export const ENABLE_ADMIN_FLATE = "mulighetsrommet.enable-admin-flate";

export const ALL_TOGGLES = [ENABLE_ADMIN_FLATE] as const;

export type Features = Record<typeof ALL_TOGGLES[number], boolean>;

export const initialFeatures: Features = {
  "mulighetsrommet.enable-admin-flate": true,
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
