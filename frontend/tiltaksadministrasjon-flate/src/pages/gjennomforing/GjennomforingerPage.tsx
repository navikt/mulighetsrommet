import { GjennomforingFilter } from "@/components/filter/GjennomforingFilter";
import { GjennomforingFilterTags } from "@/components/filter/GjennomforingFilterTags";
import { GjennomforingTable } from "@/components/gjennomforing/GjennomforingTable";
import { GjennomforingAvtaleIkon } from "@/components/ikoner/GjennomforingAvtaleIkon";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import {
  LagredeFilterOversikt,
  LagreFilterButton,
  ListSkeleton,
  useOpenFilterWhenThreshold,
} from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { Suspense, useState } from "react";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { useGjennomforingerSavedFilterState } from "@/filter/useSavedFiltersState";

export function GjennomforingerPage() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);

  const {
    filter,
    updateFilter,
    resetFilterToDefault,
    selectFilter,
    hasChanged,
    filters,
    saveFilter,
    deleteFilter,
    setDefaultFilter,
  } = useGjennomforingerSavedFilterState();

  return (
    <>
      <title>Gjennomføringer</title>
      <HeaderBanner heading="Oversikt over gjennomføringer" ikon={<GjennomforingAvtaleIkon />} />
      <ContentBox>
        <FilterAndTableLayout
          filter={
            <GjennomforingFilter
              filter={filter.values}
              updateFilter={updateFilter}
              lagredeFilterOversikt={
                <LagredeFilterOversikt
                  filters={filters}
                  selectedFilterId={filter.id}
                  onSelectFilterId={selectFilter}
                  onDeleteFilter={deleteFilter}
                  onSetDefaultFilter={setDefaultFilter}
                />
              }
            />
          }
          nullstillFilterButton={
            hasChanged ? (
              <>
                <NullstillFilterKnapp onClick={resetFilterToDefault} />
                <LagreFilterButton filter={filter.values} onLagre={saveFilter} />
              </>
            ) : null
          }
          tags={
            <GjennomforingFilterTags
              filter={filter.values}
              updateFilter={updateFilter}
              filterOpen={filterOpen}
              setTagsHeight={setTagsHeight}
            />
          }
          buttons={null}
          table={
            <ReloadAppErrorBoundary>
              <Suspense fallback={<ListSkeleton />}>
                <GjennomforingTable
                  filter={filter.values}
                  updateFilter={updateFilter}
                  tagsHeight={tagsHeight}
                  filterOpen={filterOpen}
                />
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
