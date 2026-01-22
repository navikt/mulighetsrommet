import { NavEnhetFilterTag as MultiValueFilterTag } from "@mr/frontend-common/components/filter/filterTag/NavEnhetFilterTag";
import { useRegioner } from "@/api/queries/useRegioner";
import { NavRegionDto } from "@api-client";

interface Props {
  navEnheter: string[];
  onClose: () => void;
}

export function NavEnhetFilterTag({ navEnheter, onClose }: Props) {
  const { data: regioner } = useRegioner();
  const labels = getSelectedNavEnheter(regioner, navEnheter);
  return <MultiValueFilterTag navEnheter={labels} onClose={onClose} />;
}

function getSelectedNavEnheter(regioner: NavRegionDto[], enheter: string[]): string[] {
  return regioner
    .flatMap((region) => region.enheter)
    .filter((enhet) => enheter.includes(enhet.enhetsnummer))
    .map((enhet) => enhet.navn);
}
