import { GjennomforingAvtaleIkon } from "@/components/ikoner/GjennomforingAvtaleIkon";
import { TiltakDokumentTabell } from "@/components/tiltak-dokument/TiltakDokumentTabell";
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

export function TiltakDokumenterPage() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const { filter, updateFilter, resetToDefault, hasChanged } = useFilterState(
    tiltakDokumentFilterStateAtom,
  );

  return (
    <>
      <title>Tiltaksdokumenter</title>
      <HeaderBanner heading="Oversikt over tiltaksdokumenter" ikon={<GjennomforingAvtaleIkon />} />
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
                <TiltakDokumentTabell filter={filter.values} />
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
