import { TiltakstypeFilter } from "@/components/filter/TiltakstypeFilter";
import { useTiltakstyperForGjennomforinger } from "@/api/tiltakstyper/useTiltakstyperForGjennomforinger";

interface Props {
  value: string[];
  onChange: (tiltakstyper: string[]) => void;
}

export function GjennomforingTiltakstypeFilter({ value, onChange }: Props) {
  const tiltakstyper = useTiltakstyperForGjennomforinger();
  return <TiltakstypeFilter tiltakstyper={tiltakstyper} value={value} onChange={onChange} />;
}
