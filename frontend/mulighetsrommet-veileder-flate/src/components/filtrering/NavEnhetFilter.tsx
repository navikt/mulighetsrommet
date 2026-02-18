import { Kontorstruktur, KontorstrukturKontortype } from "@api-client";
import { CheckboxGroup } from "@mr/frontend-common";
import { useNavKontorstruktur } from "@/api/queries/useNavKontorstruktur";

interface Props {
  value: string[];
  onChange: (value: string[]) => void;
}

export function NavEnhetFilter({ value, onChange }: Props) {
  const { data: regioner } = useNavKontorstruktur();
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
