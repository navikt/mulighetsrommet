import { AvtaleFilter } from "@/components/filter/AvtaleFilter";
import { AvtaleFilterButtons } from "@/components/filter/AvtaleFilterButtons";
import { AvtaleFilterTags } from "@/components/filter/AvtaleFilterTags";
import { AvtaleTabell } from "@/components/tabell/AvtaleTabell";
import { useGetTiltakstypeIdFromUrlOrThrow } from "@/hooks/useGetTiltakstypeIdFromUrl";
import { ContentBox } from "@/layouts/ContentBox";
import { useOpenFilterWhenThreshold } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { useState } from "react";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { getAvtalerForTiltakstypeFilterAtom } from "@/pages/avtaler/filter";
import { useFilterState } from "@/filter/useFilterState";

export function AvtalerForTiltakstypePage() {
  const tiltakstypeId = useGetTiltakstypeIdFromUrlOrThrow();

  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);

  const filterAtom = getAvtalerForTiltakstypeFilterAtom(tiltakstypeId);
  const { filter, updateFilter, resetToDefault, hasChanged } = useFilterState(filterAtom);

  return (
    <>
      <ContentBox>
        <FilterAndTableLayout
          filter={
            <AvtaleFilter
              filter={filter.values}
              updateFilter={updateFilter}
              skjulFilter={{
                tiltakstype: true,
              }}
            />
          }
          nullstillFilterButton={
            hasChanged ? <NullstillFilterKnapp onClick={resetToDefault} /> : null
          }
          tags={
            <AvtaleFilterTags
              filter={filter.values}
              updateFilter={updateFilter}
              tiltakstypeId={tiltakstypeId}
              filterOpen={filterOpen}
              setTagsHeight={setTagsHeight}
            />
          }
          buttons={<AvtaleFilterButtons />}
          table={
            <AvtaleTabell
              filter={filter.values}
              updateFilter={updateFilter}
              tagsHeight={tagsHeight}
              filterOpen={filterOpen}
            />
          }
          filterOpen={filterOpen}
          setFilterOpen={setFilterOpen}
        />
      </ContentBox>
      <TilToppenKnapp />
    </>
  );
}
