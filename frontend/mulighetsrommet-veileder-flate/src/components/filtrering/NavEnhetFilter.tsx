import { NavRegionDto } from "@api-client";
import { CheckboxGroup } from "@mr/frontend-common";
import { useRegioner } from "@/api/queries/useRegioner";

interface Props {
  value: string[];
  onChange: (value: string[]) => void;
}

export function NavEnhetFilter({ value, onChange }: Props) {
  const { data: regioner } = useRegioner();
  const groups = toCheckboxGroups(regioner);
  return <CheckboxGroup value={value} onChange={onChange} groups={groups} />;
}

function toCheckboxGroups(regioner: NavRegionDto[]): CheckboxGroup[] {
  return regioner.map((region) => {
    return {
      id: region.enhetsnummer,
      navn: region.navn,
      items: region.enheter.map((enhet) => ({
        id: enhet.enhetsnummer,
        navn: enhet.navn,
        erStandardvalg: enhet.erStandardvalg,
      })),
    };
  });
}
