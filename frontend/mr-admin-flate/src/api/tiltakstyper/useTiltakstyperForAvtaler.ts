import { TiltakstypeFeature } from "@tiltaksadministrasjon/api-client";
import { TiltakstypeFilterType } from "@/pages/tiltakstyper/filter";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { hasFeature } from "@/utils/tiltakstype";

export function useTiltakstyperForAvtaler(filter: TiltakstypeFilterType = {}) {
  const { data: tiltakstyper } = useTiltakstyper(filter);

  return tiltakstyper.filter((tiltakstype) =>
    hasFeature(tiltakstype, TiltakstypeFeature.KAN_OPPRETTE_AVTALE),
  );
}
