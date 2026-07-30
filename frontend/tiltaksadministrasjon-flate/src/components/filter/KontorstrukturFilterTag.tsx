import { Chips } from "@navikt/ds-react";
import { Kontorstruktur, RegionKostnadssteder } from "@tiltaksadministrasjon/api-client";
import { useKontorstruktur } from "@/api/enhet/useKontorstruktur";
import { useKostnadssteder } from "@/api/enhet/useKostnadssteder";

interface Props {
  navEnheter: string[];
  onClick: () => void;
}

export function KontorstrukturFilterTag({ navEnheter, onClick }: Props) {
  const { data: regioner } = useKontorstruktur();
  const { data: kostnadssteder } = useKostnadssteder();

  const labels = getSelectedNavEnheter(regioner, kostnadssteder, navEnheter);
  return <Chips.Removable onClick={onClick}>{tagLabel(labels)}</Chips.Removable>;
}

function getSelectedNavEnheter(
  regioner: Kontorstruktur[],
  kostnadssteder: RegionKostnadssteder[],
  enheter: string[],
): string[] {
  const matches = regioner
    .flatMap(({ kontorer }) => kontorer)
    .filter((enhet) => enheter.includes(enhet.enhetsnummer))
    .map((enhet) => enhet.navn)
    .concat(
      kostnadssteder
        .flatMap(({ kostnadssteder }) => kostnadssteder)
        .filter((enhet) => enheter.includes(enhet.enhetsnummer))
        .map((enhet) => enhet.navn),
    );

  return [...new Set(matches)];
}

function tagLabel(labels: string[]) {
  const firstLabel = labels[0];
  if (labels.length > 1) {
    return `${firstLabel} +${labels.length - 1}`;
  }
  return firstLabel;
}
