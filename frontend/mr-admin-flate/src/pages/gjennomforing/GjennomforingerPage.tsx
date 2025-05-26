import { useLagredeFilter } from "@/api/lagret-filter/useLagredeFilter";
import { GjennomforingFilter } from "@/components/filter/GjennomforingFilter";
import { GjennomforingFilterButtons } from "@/components/filter/GjennomforingFilterButtons";
import { GjennomforingFilterTags } from "@/components/filter/GjennomforingFilterTags";
import { GjennomforingTable } from "@/components/gjennomforing/GjennomforingTable";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { LagretFilterType } from "@mr/api-client-v2";
import {
  LagredeFilterOversikt,
  LagreFilterButton,
  useOpenFilterWhenThreshold,
} from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { useState } from "react";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import {
  GjennomforingFilterSchema,
  gjennomforingFilterStateAtom,
  GjennomforingFilterType,
} from "@/pages/gjennomforing/filter";
import { useFilterStateWithSavedFilters } from "@/filter/useFilterStateWithSavedFilters";

export function GjennomforingerPage() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);

  const { lagredeFilter, lagreFilter, slettFilter, setDefaultFilter } = useLagredeFilter(
    LagretFilterType.GJENNOMFORING,
  );
  const { filter, setFilter, updateFilter, resetToDefault, selectFilter, hasChanged } =
    useFilterStateWithSavedFilters(gjennomforingFilterStateAtom, lagredeFilter);

  return (
    <main>
      <title>Gjennomføringer</title>
      <HeaderBanner heading="Oversikt over gjennomføringer" ikon={<GjennomforingIkon />} />
      <ContentBox>
        <FilterAndTableLayout
          filter={<GjennomforingFilter filter={filter.values} updateFilter={updateFilter} />}
          nullstillFilterButton={
            <>
              {hasChanged ? <NullstillFilterKnapp onClick={resetToDefault} /> : null}
              <LagreFilterButton filter={filter.values} onLagre={lagreFilter} />
            </>
          }
          lagredeFilter={
            <LagredeFilterOversikt
              selectedFilterId={filter.id}
              onSelectFilterId={selectFilter}
              filter={filter.values}
              lagredeFilter={lagredeFilter}
              onSetFilter={(filter) => {
                setFilter(filter as GjennomforingFilterType);
              }}
              onDeleteFilter={slettFilter}
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
          buttons={<GjennomforingFilterButtons />}
          table={
            <ReloadAppErrorBoundary>
              <GjennomforingTable
                filter={filter.values}
                updateFilter={updateFilter}
                tagsHeight={tagsHeight}
                filterOpen={filterOpen}
              />
            </ReloadAppErrorBoundary>
          }
          filterOpen={filterOpen}
          setFilterOpen={setFilterOpen}
        />
      </ContentBox>
      <TilToppenKnapp />
    </main>
  );
}
