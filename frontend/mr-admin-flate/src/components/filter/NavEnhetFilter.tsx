import { CheckboxGroup } from "@mr/frontend-common";
import { Kontorstruktur, KontorstrukturKontortype } from "@tiltaksadministrasjon/api-client";

interface Props {
  value: string[];
  onChange: (value: string[]) => void;
  regioner: Kontorstruktur[];
}

export function NavEnhetFilter({ value, onChange, regioner }: Props) {
  const groups = toCheckboxGroups(regioner);
  return <CheckboxGroup value={value} onChange={onChange} groups={groups} />;
}

function toCheckboxGroups(regioner: Kontorstruktur[]): CheckboxGroup[] {
  return regioner.map(({ region, kontorer }) => {
    return {
      id: region.enhetsnummer,
      navn: region.navn,
      items: kontorer.map((enhet) => ({
        id: enhet.enhetsnummer,
        navn: enhet.navn,
        erStandardvalg: enhet.type === KontorstrukturKontortype.LOKAL,
      })),
    };
  });
}
