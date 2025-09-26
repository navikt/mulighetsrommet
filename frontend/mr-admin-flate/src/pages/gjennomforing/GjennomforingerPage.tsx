import { GjennomforingFilter } from "@/components/filter/GjennomforingFilter";
import { GjennomforingFilterTags } from "@/components/filter/GjennomforingFilterTags";
import { GjennomforingTable } from "@/components/gjennomforing/GjennomforingTable";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { LagretFilterType } from "@tiltaksadministrasjon/api-client";
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
import {
  GjennomforingFilterSchema,
  gjennomforingFilterStateAtom,
} from "@/pages/gjennomforing/filter";
import { useSavedFiltersState } from "@/filter/useSavedFiltersState";

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
  } = useSavedFiltersState(gjennomforingFilterStateAtom, LagretFilterType.GJENNOMFORING);

  return (
    <>
      <title>Gjennomføringer</title>
      <HeaderBanner heading="Oversikt over gjennomføringer" ikon={<GjennomforingIkon />} />
      <ContentBox>
        <FilterAndTableLayout
          filter={<GjennomforingFilter filter={filter.values} updateFilter={updateFilter} />}
          nullstillFilterButton={
            hasChanged ? (
              <>
                <NullstillFilterKnapp onClick={resetFilterToDefault} />
                <LagreFilterButton filter={filter.values} onLagre={saveFilter} />
              </>
            ) : null
          }
          lagredeFilter={
            <LagredeFilterOversikt
              filters={filters}
              selectedFilterId={filter.id}
              onSelectFilterId={selectFilter}
              onDeleteFilter={deleteFilter}
              onSetDefaultFilter={setDefaultFilter}
              validateFilterStructure={(filter) => {
                return GjennomforingFilterSchema.safeParse(filter).success;
              }}
            />
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
