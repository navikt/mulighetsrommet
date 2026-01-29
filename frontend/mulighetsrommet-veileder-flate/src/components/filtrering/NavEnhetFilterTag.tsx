import { MultiLabelFilterTag } from "@mr/frontend-common/components/filter/filterTag/MultiLabelFilterTag";
import { useRegioner } from "@/api/queries/useRegioner";
import { NavRegionDto } from "@api-client";

interface Props {
  navEnheter: string[];
  onClose: () => void;
}

export function NavEnhetFilterTag({ navEnheter, onClose }: Props) {
  const { data: regioner } = useRegioner();
  const labels = getSelectedNavEnheter(regioner, navEnheter);
  return <MultiLabelFilterTag labels={labels} onClose={onClose} />;
}

function getSelectedNavEnheter(regioner: NavRegionDto[], enheter: string[]): string[] {
  return regioner
    .flatMap((region) => region.enheter)
    .filter((enhet) => enheter.includes(enhet.enhetsnummer))
    .map((enhet) => enhet.navn);
}
