import { TiltakstypeDto, TiltakstypeFeature } from "@tiltaksadministrasjon/api-client";

export function hasFeature(tiltalstype: TiltakstypeDto, feature: TiltakstypeFeature): boolean {
  return tiltalstype.features.includes(feature);
}
