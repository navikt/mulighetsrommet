import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { TiltakstypeFilter } from "@/components/filter/TiltakstypeFilter";

interface Props {
  value: string[];
  onChange: (tiltakstyper: string[]) => void;
}

export function GjennomforingTiltakstypeFilter({ value, onChange }: Props) {
  const { data: tiltakstyper } = useTiltakstyper();
  return <TiltakstypeFilter tiltakstyper={tiltakstyper} value={value} onChange={onChange} />;
}
