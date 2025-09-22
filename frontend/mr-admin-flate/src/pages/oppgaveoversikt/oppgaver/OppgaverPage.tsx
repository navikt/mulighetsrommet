import { LagretFilterType } from "@tiltaksadministrasjon/api-client";
import {
  LagredeFilterOversikt,
  LagreFilterButton,
  ListSkeleton,
  useOpenFilterWhenThreshold,
} from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { Suspense, useState } from "react";
import { OppgaverFilter } from "@/components/filter/OppgaverFilter";
import { OppgaveFilterTags } from "@/components/filter/OppgaverFilterTags";
import { ContentBox } from "@/layouts/ContentBox";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import {
  OppgaverFilterSchema,
  oppgaverFilterStateAtom,
} from "@/pages/oppgaveoversikt/oppgaver/filter";
import { useSavedFiltersState } from "@/filter/useSavedFiltersState";
import OppgaverList from "@/components/oppgaver/OppgaveList";

export function OppgaverPage() {
  const [tagsHeight, setTagsHeight] = useState(0);
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);

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
  } = useSavedFiltersState(oppgaverFilterStateAtom, LagretFilterType.OPPGAVE);

  return (
    <ContentBox>
      <FilterAndTableLayout
        filter={<OppgaverFilter filter={filter.values} updateFilter={updateFilter} />}
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
              return OppgaverFilterSchema.safeParse(filter).success;
            }}
          />
        }
        tags={
          <OppgaveFilterTags
            filter={filter.values}
            updateFilter={updateFilter}
            filterOpen={filterOpen}
            setTagsHeight={setTagsHeight}
          />
        }
        buttons={null}
        table={
          <Suspense fallback={<ListSkeleton />}>
            <OppgaverList tagsHeight={tagsHeight} filterOpen={filterOpen} />
          </Suspense>
        }
        filterOpen={filterOpen}
        setFilterOpen={setFilterOpen}
      />
    </ContentBox>
  );
}
