import { MultiLabelFilterTag } from "@mr/frontend-common";
import { NavRegionDto } from "@tiltaksadministrasjon/api-client";

interface Props {
  navEnheter: string[];
  onClose: () => void;
  regioner: NavRegionDto[];
}

export function NavEnhetFilterTag({ navEnheter, onClose, regioner }: Props) {
  const labels = getSelectedNavEnheter(regioner, navEnheter);
  return <MultiLabelFilterTag labels={labels} onClose={onClose} />;
}

function getSelectedNavEnheter(regioner: NavRegionDto[], enheter: string[]): string[] {
  return regioner
    .flatMap((region) => region.enheter)
    .filter((enhet) => enheter.includes(enhet.enhetsnummer))
    .map((enhet) => enhet.navn);
}
