import { RegionKostnadssteder } from "@tiltaksadministrasjon/api-client";
import { useKostnadssteder } from "@/api/enhet/useKostnadssteder";
import { Chips } from "@navikt/ds-react";

interface Props {
  kostnadssteder: string[];
  onClose: () => void;
}

export function KostnadsstedFilterTag({ kostnadssteder, onClose }: Props) {
  const { data: regioner } = useKostnadssteder();
  const labels = getSelected(regioner, kostnadssteder);
  return <Chips.Removable onClick={onClose}>{tagLabel(labels)}</Chips.Removable>;
}

function getSelected(regioner: RegionKostnadssteder[], enheter: string[]): string[] {
  return regioner
    .flatMap(({ kostnadssteder }) => kostnadssteder)
    .filter((kontor) => enheter.includes(kontor.enhetsnummer))
    .map((enhet) => enhet.navn);
}

function tagLabel(labels: string[]) {
  const firstLabel = labels[0];
  if (labels.length > 1) {
    return `${firstLabel} +${labels.length - 1}`;
  }
  return firstLabel;
}
