import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { ArrangorerFilterType } from "@/api/atoms";

interface Props {
  filter: ArrangorerFilterType;
  resetFilter: () => void;
}

export function NullstillKnappForArrangorer({ filter, resetFilter }: Props) {
  return filter.sok.length > 0 ? <NullstillFilterKnapp onClick={() => resetFilter()} /> : null;
}
