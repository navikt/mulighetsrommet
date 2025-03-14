import { arrangorerFilterAtom } from "@/api/atoms";
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

export function ArrangorerPage() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);

  return (
    <>
      <HeaderBanner heading="Arrangører" ikon={<ArrangorIkon />} />
      <title>Arrangører</title>
      <ReloadAppErrorBoundary>
        <ContentBox>
          <FilterAndTableLayout
            nullstillFilterButton={
              <NullstillKnappForArrangorer filterAtom={arrangorerFilterAtom} />
            }
            filter={<ArrangorerFilter filterAtom={arrangorerFilterAtom} />}
            tags={
              <ArrangorerFilterTags
                filterAtom={arrangorerFilterAtom}
                filterOpen={filterOpen}
                setTagsHeight={setTagsHeight}
              />
            }
            buttons={null}
            table={
              <ArrangorerTabell
                filterAtom={arrangorerFilterAtom}
                tagsHeight={tagsHeight}
                filterOpen={filterOpen}
              />
            }
            setFilterOpen={setFilterOpen}
            filterOpen={filterOpen}
          />
        </ContentBox>
      </ReloadAppErrorBoundary>
    </>
  );
}
