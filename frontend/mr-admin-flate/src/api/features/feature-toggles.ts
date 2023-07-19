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
export const SLETTE_AVTALE = "mulighetsrommet.admin-flate-slett-avtale";
export const REDIGER_TILTAKSGJENNOMFORING_ADMIN_FLATE =
  "mulighetsrommet.admin-flate-rediger-tiltaksgjennomforing";
export const SLETT_TILTAKSGJENNOMFORING_ADMIN_FLATE =
  "mulighetsrommet.admin-flate-slett-tiltaksgjennomforing";
export const LAGRE_UTKAST = "mulighetsrommet.admin-flate-lagre-utkast";
export const VIS_DELTAKERLISTE_KOMET =
  "mulighetsrommet.admin-flate-vis-deltakerliste-fra-komet";
export const VIS_AVTALENOTATER =
  "mulighetsrommet.admin-flate-vis-avtalenotater";

export const ALL_TOGGLES = [
  ENABLE_ADMIN_FLATE,
  VIS_NOKKELTALL_ADMIN_FLATE,
  OPPRETT_AVTALE_ADMIN_FLATE,
  REDIGER_AVTALE_ADMIN_FLATE,
  OPPRETT_TILTAKSGJENNOMFORING_ADMIN_FLATE,
  SLETTE_AVTALE,
  REDIGER_TILTAKSGJENNOMFORING_ADMIN_FLATE,
  SLETT_TILTAKSGJENNOMFORING_ADMIN_FLATE,
  LAGRE_UTKAST,
  VIS_DELTAKERLISTE_KOMET,
  VIS_AVTALENOTATER,
] as const;

export type Features = Record<(typeof ALL_TOGGLES)[number], boolean>;
// export interface Features {
//   [ENABLE_ADMIN_FLATE]: boolean;
//   [VIS_NOKKELTALL_ADMIN_FLATE]: boolean;
//   [VIS_JOYRIDE]: boolean;
// }

export const initialFeatures: Features = {
  "mulighetsrommet.enable-admin-flate": false,
  "mulighetsrommet.admin-flate-vis-nokkeltall": false,
  "mulighetsrommet.admin-flate-opprett-avtale": false,
  "mulighetsrommet.admin-flate-rediger-avtale": false,
  "mulighetsrommet.admin-flate-opprett-tiltaksgjennomforing": false,
  "mulighetsrommet.admin-flate-slett-avtale": false,
  "mulighetsrommet.admin-flate-slett-tiltaksgjennomforing": false,
  "mulighetsrommet.admin-flate-rediger-tiltaksgjennomforing": false,
  "mulighetsrommet.admin-flate-lagre-utkast": false,
  "mulighetsrommet.admin-flate-vis-deltakerliste-fra-komet": false,
  "mulighetsrommet.admin-flate-vis-avtalenotater": false,
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
