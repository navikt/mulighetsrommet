import { TiltakDokumentTable } from "@/components/tiltak-dokument/TiltakDokumentTable";
import { TiltakDokumentFilter } from "@/components/tiltak-dokument/TiltakDokumentFilter";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { ListSkeleton, useOpenFilterWhenThreshold } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { Suspense } from "react";
import { tiltakDokumentFilterStateAtom } from "@/pages/tiltak-dokument/filter";
import { useFilterState } from "@/filter/useFilterState";
import { TiltakDokumentIkon } from "@/components/ikoner/TiltakDokumentIkon";

export function TiltakDokumenterPage() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const { filter, updateFilter, resetToDefault, hasChanged } = useFilterState(
    tiltakDokumentFilterStateAtom,
  );

  return (
    <>
      <title>Tiltaksdokumenter</title>
      <HeaderBanner heading="Oversikt over tiltaksdokumenter" ikon={<TiltakDokumentIkon />} />
      <ContentBox>
        <FilterAndTableLayout
          filter={<TiltakDokumentFilter filter={filter.values} updateFilter={updateFilter} />}
          nullstillFilterButton={
            hasChanged ? <NullstillFilterKnapp onClick={resetToDefault} /> : null
          }
          tags={null}
          buttons={null}
          table={
            <ReloadAppErrorBoundary>
              <Suspense fallback={<ListSkeleton />}>
                <TiltakDokumentTable filter={filter.values} />
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
