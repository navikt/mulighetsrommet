import {
  CheckboxDropdownGroup,
  CheckboxGroupItem,
  type CheckboxGroupChange,
} from "@mr/frontend-common";
import { RegionKostnadssteder } from "@tiltaksadministrasjon/api-client";
import { useKostnadssteder } from "@/api/enhet/useKostnadssteder";
import { useKostnadsstedTracking } from "@/sporing/useKostnadsstedTracking";

interface Props {
  value: string[];
  onChange: (value: string[], change?: CheckboxGroupChange) => void;
  tilgjengeligeKostnadssteder?: string[];
  sporingskontekst?: string;
}

export function KostnadsstedFilter({
  value,
  onChange,
  tilgjengeligeKostnadssteder,
  sporingskontekst,
}: Props) {
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
  const { logKostnadsstederEndret } = useKostnadsstedTracking(sporingskontekst);

  const handleChange = (nyeValg: string[], change?: CheckboxGroupChange) => {
    const kilde =
      change?.type === "group" ? (change.checked ? "velg_alle" : "fjern_alle") : "enkeltvalg";
    onChange(nyeValg, change);
    logKostnadsstederEndret(value, nyeValg, kilde);
  };

  return (
    <CheckboxDropdownGroup
      legend="Kostnadssteder"
      value={value}
      onChange={handleChange}
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
