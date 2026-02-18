import { useKontorstruktur } from "@/api/enhet/useKontorstruktur";
import { NavEnhetFilter } from "@/components/filter/NavEnhetFilter";

interface Props {
  value: string[];
  onChange: (value: string[]) => void;
}

export function KontorstrukturFilter({ value, onChange }: Props) {
  const { data: regioner } = useKontorstruktur();
  return <NavEnhetFilter value={value} onChange={onChange} regioner={regioner} />;
}
