import { RegionKostnadssteder } from "@tiltaksadministrasjon/api-client";
import { useKostnadssteder } from "@/api/enhet/useKostnadssteder";
import { Chips } from "@navikt/ds-react";
import { useKostnadsstedTracking } from "@/sporing/useKostnadsstedTracking";

interface Props {
  kostnadssteder: string[];
  onClose: () => void;
  sporingskontekst?: string;
}

export function KostnadsstedFilterTag({ kostnadssteder, onClose, sporingskontekst }: Props) {
  const { data: regioner } = useKostnadssteder();
  const { logKostnadsstederEndret } = useKostnadsstedTracking(sporingskontekst);
  const labels = getSelected(regioner, kostnadssteder);

  const handleClose = () => {
    onClose();
    logKostnadsstederEndret(kostnadssteder, [], "fjern_alle");
  };

  return <Chips.Removable onClick={handleClose}>{tagLabel(labels)}</Chips.Removable>;
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
