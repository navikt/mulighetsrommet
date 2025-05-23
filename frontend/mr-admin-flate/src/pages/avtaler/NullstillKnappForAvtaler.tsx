import { AvtaleFilterType, defaultAvtaleFilter } from "@/api/atoms";
import { LagretFilterType } from "@mr/api-client-v2";
import { LagreFilterButton } from "@mr/frontend-common/components/lagreFilter/LagreFilterButton";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { HStack } from "@navikt/ds-react";
import { WritableAtom } from "jotai";
import { useAtom } from "jotai/index";
import { useLagredeFilter } from "@/api/lagret-filter/useLagredeFilter";

interface Props {
  filterAtom: WritableAtom<AvtaleFilterType, [newValue: AvtaleFilterType], void>;
  tiltakstypeId?: string;
}

export function NullstillKnappForAvtaler({ filterAtom, tiltakstypeId }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);
  const { lagreFilter } = useLagredeFilter(LagretFilterType.AVTALE);

  return (
    <div className="grid grid-cols-[auto auto] h-[100%] items-center">
      {filter.visMineAvtaler ||
      filter.sok.length > 0 ||
      filter.navRegioner.length > 0 ||
      filter.avtaletyper.length > 0 ||
      (!tiltakstypeId && filter.tiltakstyper.length > 0) ||
      filter.statuser.length > 0 ||
      filter.personvernBekreftet !== undefined ||
      filter.arrangorer.length > 0 ? (
        <HStack gap="2">
          <NullstillFilterKnapp
            onClick={() => {
              setFilter({
                ...defaultAvtaleFilter,
                tiltakstyper: tiltakstypeId ? [tiltakstypeId] : defaultAvtaleFilter.tiltakstyper,
              });
            }}
          />
          <LagreFilterButton filter={filter} onLagre={lagreFilter} />
        </HStack>
      ) : null}
    </div>
  );
}
