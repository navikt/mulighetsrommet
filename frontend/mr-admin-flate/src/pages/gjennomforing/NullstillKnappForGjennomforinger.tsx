import { GjennomforingFilterType } from "@/api/atoms";
import { LagretFilterType } from "@mr/api-client-v2";
import { LagreFilterButton } from "@mr/frontend-common/components/lagreFilter/LagreFilterButton";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { useLagredeFilter } from "@/api/lagret-filter/useLagredeFilter";

interface Props {
  filter: GjennomforingFilterType;
  resetFilter: () => void;
}

export function NullstillKnappForGjennomforinger({ filter, resetFilter }: Props) {
  const { lagreFilter } = useLagredeFilter(LagretFilterType.GJENNOMFORING);

  return filter.visMineGjennomforinger ||
    filter.search.length > 0 ||
    filter.tiltakstyper.length > 0 ||
    filter.navEnheter.length > 0 ||
    filter.statuser.length > 0 ||
    filter.arrangorer.length > 0 ? (
    <>
      <NullstillFilterKnapp onClick={() => resetFilter()} />
      <LagreFilterButton filter={filter} onLagre={lagreFilter} />
    </>
  ) : null;
}
