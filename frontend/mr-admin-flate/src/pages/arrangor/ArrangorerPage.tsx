import { arrangorerFilterAtom, ArrangorerFilterType, defaultArrangorerFilter } from "@/api/atoms";
import { ArrangorerFilter } from "@/components/filter/ArrangorerFilter";
import { ArrangorerFilterTags } from "@/components/filter/ArrangorerFilterTags";
import { ArrangorIkon } from "@/components/ikoner/ArrangorIkon";
import { ArrangorerTabell } from "@/components/tabell/ArrangorerTabell";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { useOpenFilterWhenThreshold } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { useState } from "react";
import { NullstillKnappForArrangorer } from "./NullstillKnappForArrangorer";
import { useAtom } from "jotai";

export function ArrangorerPage() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);

  const [filter, setFilter] = useAtom(arrangorerFilterAtom);

  function updateFilter(value: Partial<ArrangorerFilterType>) {
    setFilter((prev) => ({ ...prev, ...value }));
  }

  function resetFilter() {
    setFilter(defaultArrangorerFilter);
  }

  return (
    <main>
      <title>Arrangører</title>
      <HeaderBanner heading="Arrangører" ikon={<ArrangorIkon />} />
      <ReloadAppErrorBoundary>
        <ContentBox>
          <FilterAndTableLayout
            nullstillFilterButton={
              <NullstillKnappForArrangorer filter={filter} resetFilter={resetFilter} />
            }
            filter={<ArrangorerFilter filter={filter} updateFilter={updateFilter} />}
            tags={
              <ArrangorerFilterTags
                filter={filter}
                updateFilter={updateFilter}
                filterOpen={filterOpen}
                setTagsHeight={setTagsHeight}
              />
            }
            buttons={null}
            table={
              <ArrangorerTabell
                filter={filter}
                updateFilter={updateFilter}
                tagsHeight={tagsHeight}
                filterOpen={filterOpen}
              />
            }
            setFilterOpen={setFilterOpen}
            filterOpen={filterOpen}
          />
        </ContentBox>
      </ReloadAppErrorBoundary>
    </main>
  );
}
