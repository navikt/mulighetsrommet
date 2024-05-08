import { ReloadAppErrorBoundary, useTitle } from "mulighetsrommet-frontend-common";
import { FilterAndTableLayout } from "mulighetsrommet-frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { useState } from "react";
import { arrangorerFilterAtom } from "../../api/atoms";
import { ArrangorerFilter } from "../../components/filter/ArrangorerFilter";
import { Brodsmuler } from "../../components/navigering/Brodsmuler";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { HeaderBanner } from "../../layouts/HeaderBanner";
import { MainContainer } from "../../layouts/MainContainer";
import { NullstillKnappForArrangorer } from "./NullstillKnappForArrangorer";
import { ArrangorerTabell } from "../../components/tabell/ArrangorerTabell";
import { ArrangorerFilterTags } from "../../components/filter/ArrangorerFilterTags";
import { ArrangorIkon } from "../../components/ikoner/ArrangorIkon";

export function ArrangorerPage() {
  useTitle("Arrangører");
  const [filterOpen, setFilterOpen] = useState<boolean>(true);
  const [tagsHeight, setTagsHeight] = useState(0);

  return (
    <>
      <Brodsmuler
        brodsmuler={[
          { tittel: "Forside", lenke: "/" },
          { tittel: "Arrangører", lenke: "/arrangorer" },
        ]}
      />
      <HeaderBanner heading="Arrangører" ikon={<ArrangorIkon />} />
      <ReloadAppErrorBoundary>
        <MainContainer>
          <ContainerLayout>
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
          </ContainerLayout>
        </MainContainer>
      </ReloadAppErrorBoundary>
    </>
  );
}
