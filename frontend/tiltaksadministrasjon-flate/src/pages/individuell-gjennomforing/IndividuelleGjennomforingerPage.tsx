import { GjennomforingAvtaleIkon } from "@/components/ikoner/GjennomforingAvtaleIkon";
import { IndividuellGjennomforingTabell } from "@/components/individuell-gjennomforing/IndividuellGjennomforingTabell";
import { IndividuellGjennomforingFilter } from "@/components/individuell-gjennomforing/IndividuellGjennomforingFilter";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { ListSkeleton, useOpenFilterWhenThreshold } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { Suspense } from "react";
import { individuellGjennomforingFilterStateAtom } from "@/pages/individuell-gjennomforing/filter";
import { useFilterState } from "@/filter/useFilterState";

export function IndividuelleGjennomforingerPage() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const { filter, updateFilter, resetToDefault, hasChanged } = useFilterState(
    individuellGjennomforingFilterStateAtom,
  );

  return (
    <>
      <title>Individuelle gjennomføringer</title>
      <HeaderBanner
        heading="Oversikt over individuelle gjennomføringer"
        ikon={<GjennomforingAvtaleIkon />}
      />
      <ContentBox>
        <FilterAndTableLayout
          filter={
            <IndividuellGjennomforingFilter
              filter={filter.values}
              updateFilter={updateFilter}
            />
          }
          nullstillFilterButton={
            hasChanged ? <NullstillFilterKnapp onClick={resetToDefault} /> : null
          }
          tags={null}
          buttons={null}
          table={
            <ReloadAppErrorBoundary>
              <Suspense fallback={<ListSkeleton />}>
                <IndividuellGjennomforingTabell filter={filter.values} />
              </Suspense>
            </ReloadAppErrorBoundary>
          }
          filterOpen={filterOpen}
          setFilterOpen={setFilterOpen}
        />
      </ContentBox>
      <TilToppenKnapp />
    </>
  );
}
