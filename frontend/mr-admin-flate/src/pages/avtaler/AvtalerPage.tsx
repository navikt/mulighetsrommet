import { AvtaleFilter } from "@/components/filter/AvtaleFilter";
import { AvtaleFilterButtons } from "@/components/filter/AvtaleFilterButtons";
import { AvtaleFilterTags } from "@/components/filter/AvtaleFilterTags";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { AvtaleTabell } from "@/components/tabell/AvtaleTabell";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { LagretFilterType } from "@tiltaksadministrasjon/api-client";
import {
  LagredeFilterOversikt,
  LagreFilterButton,
  useOpenFilterWhenThreshold,
} from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { useState } from "react";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { AvtaleFilterSchema, avtalerFilterStateAtom } from "@/pages/avtaler/filter";
import { useSavedFiltersState } from "@/filter/useSavedFiltersState";

export function AvtalerPage() {
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
  } = useSavedFiltersState(avtalerFilterStateAtom, LagretFilterType.AVTALE);

  return (
    <>
      <title>Avtaler</title>
      <HeaderBanner heading="Oversikt over avtaler" harUndermeny ikon={<AvtaleIkon />} />
      <ReloadAppErrorBoundary>
        <ContentBox>
          <FilterAndTableLayout
            filter={<AvtaleFilter filter={filter.values} updateFilter={updateFilter} />}
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
                  return AvtaleFilterSchema.safeParse(filter).success;
                }}
              />
            }
            tags={
              <AvtaleFilterTags
                filter={filter.values}
                updateFilter={updateFilter}
                filterOpen={filterOpen}
                setTagsHeight={setTagsHeight}
              />
            }
            buttons={<AvtaleFilterButtons />}
            table={
              <AvtaleTabell
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
      <TilToppenKnapp />
    </>
  );
}
