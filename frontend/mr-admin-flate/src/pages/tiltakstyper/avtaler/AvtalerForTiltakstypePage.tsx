import {
  AvtaleFilterType,
  defaultAvtaleFilter,
  getAvtalerForTiltakstypeFilterAtomFamily,
} from "@/api/atoms";
import { AvtaleFilter } from "@/components/filter/AvtaleFilter";
import { AvtaleFilterButtons } from "@/components/filter/AvtaleFilterButtons";
import { AvtaleFilterTags } from "@/components/filter/AvtaleFilterTags";
import { AvtaleTabell } from "@/components/tabell/AvtaleTabell";
import { useGetTiltakstypeIdFromUrlOrThrow } from "@/hooks/useGetTiltakstypeIdFromUrl";
import { ContentBox } from "@/layouts/ContentBox";
import { NullstillKnappForAvtaler } from "@/pages/avtaler/NullstillKnappForAvtaler";
import { useOpenFilterWhenThreshold } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { useState } from "react";
import { useAtom } from "jotai";

export function AvtalerForTiltakstypePage() {
  const tiltakstypeId = useGetTiltakstypeIdFromUrlOrThrow();
  const filterAtom = getAvtalerForTiltakstypeFilterAtomFamily(tiltakstypeId);
  const [filter, setFilter] = useAtom(filterAtom);
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);

  function updateFilter(value: Partial<AvtaleFilterType>) {
    setFilter({ ...filter, ...value });
  }

  function resetFilter() {
    setFilter({ ...defaultAvtaleFilter, tiltakstyper: [tiltakstypeId] });
  }

  return (
    <>
      <ContentBox>
        <FilterAndTableLayout
          filter={
            <AvtaleFilter
              filter={filter}
              updateFilter={updateFilter}
              skjulFilter={{
                tiltakstype: true,
              }}
            />
          }
          tags={
            <AvtaleFilterTags
              filter={filter}
              updateFilter={updateFilter}
              tiltakstypeId={tiltakstypeId}
              filterOpen={filterOpen}
              setTagsHeight={setTagsHeight}
            />
          }
          buttons={<AvtaleFilterButtons />}
          table={
            <AvtaleTabell
              filter={filter}
              updateFilter={updateFilter}
              tagsHeight={tagsHeight}
              filterOpen={filterOpen}
            />
          }
          filterOpen={filterOpen}
          setFilterOpen={setFilterOpen}
          nullstillFilterButton={
            <NullstillKnappForAvtaler
              filter={filter}
              resetFilter={resetFilter}
              tiltakstypeId={tiltakstypeId}
            />
          }
        />
      </ContentBox>
      <TilToppenKnapp />
    </>
  );
}
