import { useNavKontorstruktur } from "@/api/queries/useNavKontorstruktur";
import { Kontorstruktur } from "@api-client";
import { Chips } from "@navikt/ds-react";

interface Props {
  navEnheter: string[];
  onClose: () => void;
}

export function NavEnhetFilterTag({ navEnheter, onClose }: Props) {
  const { data: regioner } = useNavKontorstruktur();
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
