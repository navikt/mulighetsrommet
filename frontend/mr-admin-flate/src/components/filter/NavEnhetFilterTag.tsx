import { NavRegionDto } from "@tiltaksadministrasjon/api-client";
import { Chips } from "node_modules/@navikt/ds-react/esm/chips/Chips";

interface Props {
  navEnheter: string[];
  onClose: () => void;
  regioner: NavRegionDto[];
}

export function NavEnhetFilterTag({ navEnheter, onClose, regioner }: Props) {
  const labels = getSelectedNavEnheter(regioner, navEnheter);
  return <Chips.Removable onClick={onClose}>{tagLabel(labels)}</Chips.Removable>;
}

function getSelectedNavEnheter(regioner: NavRegionDto[], enheter: string[]): string[] {
  return regioner
    .flatMap((region) => region.enheter)
    .filter((enhet) => enheter.includes(enhet.enhetsnummer))
    .map((enhet) => enhet.navn);
}

function tagLabel(labels: string[]) {
  const firstLabel = labels[0];
  if (labels.length > 1) {
    return `${firstLabel} +${labels.length - 1}`;
  }
  return firstLabel;
}
