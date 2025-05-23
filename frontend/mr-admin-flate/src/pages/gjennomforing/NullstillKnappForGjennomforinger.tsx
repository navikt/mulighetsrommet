import { defaultGjennomforingfilter, GjennomforingFilterType } from "@/api/atoms";
import { AvtaleDto, LagretFilterType } from "@mr/api-client-v2";
import { LagreFilterButton } from "@mr/frontend-common/components/lagreFilter/LagreFilterButton";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { WritableAtom } from "jotai";
import { useAtom } from "jotai/index";
import { useLagreFilter } from "@/api/lagret-filter/useLagreFilter";

interface Props {
  filterAtom: WritableAtom<GjennomforingFilterType, [newValue: GjennomforingFilterType], void>;
  avtale?: AvtaleDto;
}

export function NullstillKnappForGjennomforinger({ filterAtom, avtale }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);
  const lagreFilterMutation = useLagreFilter();

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
      <LagreFilterButton
        filter={filter}
        onLagre={(namedFilter) => {
          lagreFilterMutation.mutate({
            ...namedFilter,
            type: LagretFilterType.GJENNOMFORING,
            isDefault: false,
            sortOrder: 0,
          });
          lagreFilterMutation.reset();
        }}
      />
    </>
  ) : null;
}
