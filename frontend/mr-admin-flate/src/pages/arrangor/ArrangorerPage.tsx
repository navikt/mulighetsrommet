import { useTitle } from "mulighetsrommet-frontend-common";
import { FilterAndTableLayout } from "mulighetsrommet-frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { useState } from "react";
import { ReloadAppErrorBoundary } from "../../ErrorBoundary";
import { arrangorerFilterAtom } from "../../api/atoms";
import { ArrangorerFilter } from "../../components/filter/ArrangorerFilter";
import { Brodsmuler } from "../../components/navigering/Brodsmuler";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { HeaderBanner } from "../../layouts/HeaderBanner";
import { MainContainer } from "../../layouts/MainContainer";
import { NullstillKnappForArrangorer } from "./NullstillKnappForArrangorer";
import { ArrangorerTabell } from "../../components/tabell/ArrangorerTabell";

export function ArrangorerPage() {
  useTitle("Arrangører");
  const [filterOpen, setFilterOpen] = useState<boolean>(true);
  // TODO Remove eslint-disable
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [tagsHeight, setTagsHeight] = useState(0);

  return (
    <>
      <Brodsmuler
        brodsmuler={[
          { tittel: "Forside", lenke: "/" },
          { tittel: "Arrangører", lenke: "/arrangorer" },
        ]}
      />
      <HeaderBanner heading="Arrangører" />
      <ReloadAppErrorBoundary>
        <MainContainer>
          <ContainerLayout>
            <FilterAndTableLayout
              nullstillFilterButton={
                <NullstillKnappForArrangorer filterAtom={arrangorerFilterAtom} />
              }
              filter={<ArrangorerFilter filterAtom={arrangorerFilterAtom} />}
              tags={null}
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
