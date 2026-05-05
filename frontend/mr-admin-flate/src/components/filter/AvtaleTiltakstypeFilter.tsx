import { useTiltakstyperForAvtaler } from "@/api/tiltakstyper/useTiltakstyperForAvtaler";
import { TiltakskodeFilter } from "@/components/filter/TiltakskodeFilter";
import { Tiltakskode } from "@tiltaksadministrasjon/api-client";

interface Props {
  value: Tiltakskode[];
  onChange: (tiltakstyper: Tiltakskode[]) => void;
}

export function AvtaleTiltakstypeFilter({ value, onChange }: Props) {
  const tiltakstyper = useTiltakstyperForAvtaler();
  return <TiltakskodeFilter tiltakstyper={tiltakstyper} value={value} onChange={onChange} />;
}
