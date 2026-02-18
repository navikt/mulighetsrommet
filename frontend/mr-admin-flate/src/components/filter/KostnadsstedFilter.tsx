import { CheckboxGroup } from "@mr/frontend-common";
import { RegionKostnadssteder } from "@tiltaksadministrasjon/api-client";
import { useKostnadssteder } from "@/api/enhet/useKostnadssteder";

interface Props {
  value: string[];
  onChange: (value: string[]) => void;
}

export function KostnadsstedFilter({ value, onChange }: Props) {
  const { data: regioner } = useKostnadssteder();
  const groups = toCheckboxGroups(regioner);
  return <CheckboxGroup value={value} onChange={onChange} groups={groups} />;
}

function toCheckboxGroups(regioner: RegionKostnadssteder[]): CheckboxGroup[] {
  return regioner.map(({ region, kostnadssteder }) => {
    return {
      id: region.enhetsnummer,
      navn: region.navn,
      items: kostnadssteder.map((enhet) => ({
        id: enhet.enhetsnummer,
        navn: enhet.navn,
        erStandardvalg: true,
      })),
    };
  });
}
