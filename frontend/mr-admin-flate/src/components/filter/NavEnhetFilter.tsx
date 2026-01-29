import { CheckboxGroup } from "@mr/frontend-common";
import { NavRegionDto } from "@tiltaksadministrasjon/api-client";

interface Props {
  value: string[];
  onChange: (value: string[]) => void;
  regioner: NavRegionDto[];
}

export function NavEnhetFilter({ value, onChange, regioner }: Props) {
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
