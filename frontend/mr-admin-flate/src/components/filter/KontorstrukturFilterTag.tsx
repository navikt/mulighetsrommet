import { Chips } from "@navikt/ds-react";
import { Kontorstruktur } from "@tiltaksadministrasjon/api-client";
import { useKontorstruktur } from "@/api/enhet/useKontorstruktur";

interface Props {
  navEnheter: string[];
  onClose: () => void;
}

export function KontorstrukturFilterTag({ navEnheter, onClose }: Props) {
  const { data: regioner } = useKontorstruktur();
  const labels = getSelectedNavEnheter(regioner, navEnheter);
  return <Chips.Removable onClick={onClose}>{tagLabel(labels)}</Chips.Removable>;
}

function getSelectedNavEnheter(regioner: Kontorstruktur[], enheter: string[]): string[] {
  return regioner
    .flatMap(({ kontorer }) => kontorer)
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
