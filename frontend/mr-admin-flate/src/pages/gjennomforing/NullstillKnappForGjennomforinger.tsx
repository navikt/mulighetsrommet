import { defaultGjennomforingfilter, GjennomforingFilterType } from "@/api/atoms";
import { AvtaleDto, LagretFilterType } from "@mr/api-client-v2";
import { LagreFilterButton } from "@mr/frontend-common/components/lagreFilter/LagreFilterButton";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { WritableAtom } from "jotai";
import { useAtom } from "jotai/index";
import { useLagredeFilter } from "@/api/lagret-filter/useLagredeFilter";

interface Props {
  filterAtom: WritableAtom<GjennomforingFilterType, [newValue: GjennomforingFilterType], void>;
  avtale?: AvtaleDto;
}

export function NullstillKnappForGjennomforinger({ filterAtom, avtale }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);
  const { lagreFilter } = useLagredeFilter(LagretFilterType.GJENNOMFORING);

  return filter.visMineGjennomforinger ||
    filter.search.length > 0 ||
    filter.tiltakstyper.length > 0 ||
    filter.navEnheter.length > 0 ||
    filter.statuser.length > 0 ||
    filter.arrangorer.length > 0 ? (
    <>
      <NullstillFilterKnapp
        onClick={() => {
          setFilter({
            ...defaultGjennomforingfilter,
            avtale: avtale?.id ?? defaultGjennomforingfilter.avtale,
          });
        }}
      />
      <LagreFilterButton filter={filter} onLagre={lagreFilter} />
    </>
  ) : null;
}
