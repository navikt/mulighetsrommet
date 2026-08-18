import { NavFilterTags } from "@/apps/nav/filtrering/NavFilterTags";
import { Melding } from "@/components/melding/Melding";
import { FilterMenu } from "@/components/filtrering/FilterMenu";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { ArbeidsmarkedstiltakList } from "@/components/oversikt/ArbeidsmarkedstiltakList";
import { useNavArbeidsmarkedstiltak } from "@/api/queries/useArbeidsmarkedstiltak";
import {
  isFilterReady,
  useArbeidsmarkedstiltakFilterUtenBrukerIKontekst,
} from "@/hooks/useArbeidsmarkedstiltakFilter";
import { LagredeFilterOversikt, LagreFilterButton, ListSkeleton } from "@mr/frontend-common";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import { TilToppenKnapp } from "@mr/frontend-common/components/tilToppenKnapp/TilToppenKnapp";
import { Suspense, useState } from "react";

export function NavArbeidsmarkedstiltakOversikt() {
  const [filterOpen, setFilterOpen] = useState(true);
  const [tagsHeight, setTagsHeight] = useState(0);

  const {
    filter,
    filterHasChanged,
    selectedFilterId,
    selectFilter,
    savedFilters,
    resetFilterToDefaults,
    saveFilter,
    setDefaultFilter,
    deleteFilter,
  } = useArbeidsmarkedstiltakFilterUtenBrukerIKontekst();

  const { data: tiltak = [], isPending } = useNavArbeidsmarkedstiltak();

  return (
    <>
      <FilterAndTableLayout
        hasChanged={filterHasChanged}
        filterOpen={filterOpen}
        setFilterOpen={setFilterOpen}
        buttons={null}
        filter={
          <Suspense fallback={<div>loading...</div>}>
            <FilterMenu
              lagredeFilterOversikt={
                <LagredeFilterOversikt
                  filters={savedFilters}
                  selectedFilterId={selectedFilterId}
                  onSelectFilterId={selectFilter}
                  onDeleteFilter={deleteFilter}
                  onSetDefaultFilter={setDefaultFilter}
                />
              }
            />
          </Suspense>
        }
        tags={<NavFilterTags filterOpen={filterOpen} setTagsHeight={setTagsHeight} />}
        nullstillFilterButton={<NullstillFilterKnapp onClick={resetFilterToDefaults} />}
        lagreFilterButton={<LagreFilterButton onLagre={saveFilter} filter={filter} />}
        table={
          <ArbeidsmarkedstiltakList
            tiltak={tiltak}
            filterOpen={filterOpen}
            feilmelding={
              !isFilterReady(filter) ? (
                <Melding
                  data-testid="filter-mangler-verdier-feilmelding"
                  header="Filter mangler"
                  variant="info"
                >
                  Du må filtrere på en innsatsgruppe og minst én Nav-enhet for å se tiltak
                </Melding>
              ) : tiltak.length === 0 ? (
                isPending ? (
                  <ListSkeleton />
                ) : (
                  <Melding header="Ingen tiltak funnet" variant="warning">
                    Prøv å justere søket eller filteret for å finne det du leter etter
                  </Melding>
                )
              ) : null
            }
            tagsHeight={tagsHeight}
          />
        }
      />
      <TilToppenKnapp />
    </>
  );
}
