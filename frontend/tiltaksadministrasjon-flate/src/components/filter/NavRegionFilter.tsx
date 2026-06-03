import { CheckboxGroup } from "@mr/frontend-common";
import { useKontorstruktur } from "@/api/enhet/useKontorstruktur";

interface Props {
  value: string[];
  onChange: (value: string[]) => void;
}

export function NavRegionFilter({ value, onChange }: Props) {
  const { data: regioner } = useKontorstruktur();
  return (
    <CheckboxGroup
      legend="Nav-regioner"
      hideLegend
      value={value}
      onChange={onChange}
      items={regioner.map(({ region }) => ({ id: region.enhetsnummer, navn: region.navn }))}
    />
  );
}
