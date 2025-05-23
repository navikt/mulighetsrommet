import { GjennomforingFilterType } from "@/api/atoms";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";

interface Props {
  filter: GjennomforingFilterType;
  resetFilter: () => void;
}

export function NullstillKnappForGjennomforinger({ filter, resetFilter }: Props) {
  return filter.visMineGjennomforinger ||
    filter.search.length > 0 ||
    filter.tiltakstyper.length > 0 ||
    filter.navEnheter.length > 0 ||
    filter.statuser.length > 0 ||
    filter.arrangorer.length > 0 ? (
    <NullstillFilterKnapp onClick={() => resetFilter()} />
  ) : null;
}
