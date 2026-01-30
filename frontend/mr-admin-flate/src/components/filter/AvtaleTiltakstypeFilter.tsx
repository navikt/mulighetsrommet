import { useTiltakstyperForAvtaler } from "@/api/tiltakstyper/useTiltakstyperForAvtaler";
import { TiltakstypeFilter } from "@/components/filter/TiltakstypeFilter";

interface Props {
  value: string[];
  onChange: (tiltakstyper: string[]) => void;
}

export function AvtaleTiltakstypeFilter({ value, onChange }: Props) {
  const tiltakstyper = useTiltakstyperForAvtaler();
  return <TiltakstypeFilter tiltakstyper={tiltakstyper} value={value} onChange={onChange} />;
}
