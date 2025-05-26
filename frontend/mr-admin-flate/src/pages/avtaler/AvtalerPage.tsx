import { useLagredeFilter } from "@/api/lagret-filter/useLagredeFilter";
import { AvtaleFilter } from "@/components/filter/AvtaleFilter";
import { AvtaleFilterButtons } from "@/components/filter/AvtaleFilterButtons";
import { AvtaleFilterTags } from "@/components/filter/AvtaleFilterTags";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { AvtaleTabell } from "@/components/tabell/AvtaleTabell";
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
  AvtaleFilterSchema,
  AvtaleFilterType,
  avtalerFilterStateAtom,
} from "@/pages/avtaler/filter";
import { useFilterStateWithSavedFilters } from "@/filter/useFilterStateWithSavedFilters";

export function AvtalerPage() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [tagsHeight, setTagsHeight] = useState(0);

  const { lagredeFilter, lagreFilter, slettFilter, setDefaultFilter } = useLagredeFilter(
    LagretFilterType.AVTALE,
  );
  const { filter, setFilter, updateFilter, resetToDefault, selectFilter, hasChanged } =
    useFilterStateWithSavedFilters(avtalerFilterStateAtom, lagredeFilter);

  return (
    <main>
      <title>Avtaler</title>
      <HeaderBanner heading="Oversikt over avtaler" harUndermeny ikon={<AvtaleIkon />} />
      <ReloadAppErrorBoundary>
        <ContentBox>
          <FilterAndTableLayout
            filter={<AvtaleFilter filter={filter.values} updateFilter={updateFilter} />}
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
                  setFilter(filter as AvtaleFilterType);
                }}
                onDeleteFilter={slettFilter}
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
    </main>
  );
}
