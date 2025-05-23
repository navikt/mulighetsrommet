import { AvtaleFilterType } from "@/api/atoms";
import { LagretFilterType } from "@mr/api-client-v2";
import { LagreFilterButton } from "@mr/frontend-common/components/lagreFilter/LagreFilterButton";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { useLagredeFilter } from "@/api/lagret-filter/useLagredeFilter";

interface Props {
  filter: AvtaleFilterType;
  resetFilter: () => void;
  tiltakstypeId?: string;
}

export function NullstillKnappForAvtaler({ filter, resetFilter, tiltakstypeId }: Props) {
  const { lagreFilter } = useLagredeFilter(LagretFilterType.AVTALE);

  return filter.visMineAvtaler ||
    filter.sok.length > 0 ||
    filter.navRegioner.length > 0 ||
    filter.avtaletyper.length > 0 ||
    (!tiltakstypeId && filter.tiltakstyper.length > 0) ||
    filter.statuser.length > 0 ||
    filter.personvernBekreftet !== undefined ||
    filter.arrangorer.length > 0 ? (
    <>
      <NullstillFilterKnapp onClick={() => resetFilter()} />
      <LagreFilterButton filter={filter} onLagre={lagreFilter} />
    </>
  ) : null;
}
