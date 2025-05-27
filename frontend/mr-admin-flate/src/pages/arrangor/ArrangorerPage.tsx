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
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { arrangorerFilterStateAtom } from "@/pages/arrangor/filter";
import { useFilterState } from "@/filter/useFilterState";

export function ArrangorerPage() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);

  const { filter, updateFilter, resetToDefault, hasChanged } =
    useFilterState(arrangorerFilterStateAtom);

  return (
    <main>
      <title>Arrangører</title>
      <HeaderBanner heading="Arrangører" ikon={<ArrangorIkon />} />
      <ReloadAppErrorBoundary>
        <ContentBox>
          <FilterAndTableLayout
            filter={<ArrangorerFilter filter={filter.values} updateFilter={updateFilter} />}
            nullstillFilterButton={
              hasChanged ? <NullstillFilterKnapp onClick={resetToDefault} /> : null
            }
            tags={
              <ArrangorerFilterTags
                filter={filter.values}
                updateFilter={updateFilter}
                filterOpen={filterOpen}
                setTagsHeight={setTagsHeight}
              />
            }
            buttons={null}
            table={
              <ArrangorerTabell
                filter={filter.values}
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
