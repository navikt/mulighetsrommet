import { TiltakstypeEgenskap } from "@tiltaksadministrasjon/api-client";
import { TiltakstypeFilterType } from "@/pages/tiltakstyper/filter";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { harEgenskap } from "@/utils/tiltakstype";

export function useTiltakstyperForAvtaler(filter: TiltakstypeFilterType = {}) {
  const { data: tiltakstyper } = useTiltakstyper(filter);

  return tiltakstyper.filter((tiltakstype) =>
    harEgenskap(tiltakstype, TiltakstypeEgenskap.KAN_OPPRETTE_AVTALE),
  );
}
