import { CheckboxDropdownGroup, CheckboxGroupItem } from "@mr/frontend-common";
import { RegionKostnadssteder } from "@tiltaksadministrasjon/api-client";
import { useKostnadssteder } from "@/api/enhet/useKostnadssteder";

interface Props {
  value: string[];
  onChange: (value: string[]) => void;
  tilgjengeligeKostnadssteder?: string[];
}

export function KostnadsstedFilter({ value, onChange, tilgjengeligeKostnadssteder }: Props) {
  const { data: regioner } = useKostnadssteder();
  const filtrerteRegioner = tilgjengeligeKostnadssteder
    ? regioner
        .map(({ region, kostnadssteder }) => ({
          region,
          kostnadssteder: kostnadssteder.filter(({ enhetsnummer }) =>
            tilgjengeligeKostnadssteder.includes(enhetsnummer),
          ),
        }))
        .filter(({ kostnadssteder }) => kostnadssteder.length > 0)
    : regioner;
  const groups = toCheckboxGroups(filtrerteRegioner);
  return (
    <CheckboxDropdownGroup
      legend="Kostnadssteder"
      value={value}
      onChange={onChange}
      items={groups}
    />
  );
}

function toCheckboxGroups(regioner: RegionKostnadssteder[]): CheckboxGroupItem[] {
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
