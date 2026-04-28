import { TiltakskodeFilter } from "@/components/filter/TiltakskodeFilter";
import { useTiltakstyperForGjennomforinger } from "@/api/tiltakstyper/useTiltakstyperForGjennomforinger";
import { Tiltakskode } from "@tiltaksadministrasjon/api-client";

interface Props {
  value: Tiltakskode[];
  onChange: (tiltakstyper: Tiltakskode[]) => void;
}

export function GjennomforingTiltakstypeFilter({ value, onChange }: Props) {
  const tiltakstyper = useTiltakstyperForGjennomforinger();
  return <TiltakskodeFilter tiltakstyper={tiltakstyper} value={value} onChange={onChange} />;
}
