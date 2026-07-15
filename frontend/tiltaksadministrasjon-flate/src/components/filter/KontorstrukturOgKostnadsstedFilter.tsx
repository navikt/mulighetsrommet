import { useKontorstruktur } from "@/api/enhet/useKontorstruktur";
import { useKostnadssteder } from "@/api/enhet/useKostnadssteder";
import { CheckboxGroup, CheckboxGroupItem } from "@mr/frontend-common";
import {
  Kontorstruktur,
  KontorstrukturKontortype,
  RegionKostnadssteder,
} from "@tiltaksadministrasjon/api-client";

interface Props {
  value: string[];
  onChange: (value: string[]) => void;
}

export function KontorstrukturOgKostnadsstedFilter({ value, onChange }: Props) {
  const { data: regioner } = useKontorstruktur();
  const { data: kostnadsstedRegioner } = useKostnadssteder();
  const groups = toCheckboxGroups(regioner, kostnadsstedRegioner);
  return (
    <CheckboxGroup
      legend="Nav-kontorer"
      hideLegend
      value={value}
      onChange={onChange}
      items={groups}
    />
  );
}

function toCheckboxGroups(
  regioner: Kontorstruktur[],
  kostnadsstedRegioner: RegionKostnadssteder[],
): CheckboxGroupItem[] {
  const kostnadsstedByRegion = new Map(
    kostnadsstedRegioner.map(({ region, kostnadssteder }) => [region.enhetsnummer, kostnadssteder]),
  );

  const result: CheckboxGroupItem[] = regioner.map(({ region, kontorer }) => {
    const kostnadssteder = kostnadsstedByRegion.get(region.enhetsnummer) ?? [];
    const kontorEnhetsnummer = new Set(kontorer.map((k) => k.enhetsnummer));

    return {
      id: region.enhetsnummer,
      navn: region.navn,
      items: [
        ...kontorer.map((enhet) => ({
          id: enhet.enhetsnummer,
          navn: enhet.navn,
          erStandardvalg: enhet.type === KontorstrukturKontortype.LOKAL,
        })),
        ...kostnadssteder
          .filter((ks) => !kontorEnhetsnummer.has(ks.enhetsnummer))
          .map((enhet) => ({
            id: enhet.enhetsnummer,
            navn: enhet.navn,
            erStandardvalg: true,
          })),
      ],
    };
  });

  const regionEnhetsnummer = new Set(regioner.map((r) => r.region.enhetsnummer));
  for (const { region, kostnadssteder } of kostnadsstedRegioner) {
    if (!regionEnhetsnummer.has(region.enhetsnummer)) {
      result.push({
        id: region.enhetsnummer,
        navn: region.navn,
        items: kostnadssteder.map((enhet) => ({
          id: enhet.enhetsnummer,
          navn: enhet.navn,
          erStandardvalg: true,
        })),
      });
    }
  }

  return result;
}
