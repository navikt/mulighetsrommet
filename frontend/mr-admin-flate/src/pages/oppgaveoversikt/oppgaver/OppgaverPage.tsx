import { useOppgaver } from "@/api/oppgaver/useOppgaver";
import { EmptyState } from "@/components/notifikasjoner/EmptyState";
import { Oppgave } from "@/components/oppgaver/Oppgave";
import { GetOppgaverResponse, LagretFilterType } from "@mr/api-client-v2";
import {
  LagredeFilterOversikt,
  LagreFilterButton,
  useOpenFilterWhenThreshold,
} from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { Box, HStack, Select, VStack } from "@navikt/ds-react";
import { useState } from "react";
import { OppgaverFilter } from "@/components/filter/OppgaverFilter";
import { OppgaveFilterTags } from "@/components/filter/OppgaverFilterTags";
import { ContentBox } from "@/layouts/ContentBox";
import { NullstillFilterKnapp } from "@mr/frontend-common/components/nullstillFilterKnapp/NullstillFilterKnapp";
import {
  OppgaverFilterSchema,
  oppgaverFilterStateAtom,
} from "@/pages/oppgaveoversikt/oppgaver/filter";
import { useSavedFiltersState } from "@/filter/useSavedFiltersState";

type OppgaverSorting = "nyeste" | "eldste";

function sort(oppgaver: GetOppgaverResponse, sorting: OppgaverSorting) {
  if (sorting === "nyeste") {
    return oppgaver.sort((a, b) => {
      const aDate = new Date(a.createdAt);
      const bDate = new Date(b.createdAt);

      return bDate.getTime() - aDate.getTime();
    });
  }
  if (sorting === "eldste") {
    return oppgaver.sort((a, b) => {
      const aDate = new Date(a.createdAt);
      const bDate = new Date(b.createdAt);

      return aDate.getTime() - bDate.getTime();
    });
  }

  return oppgaver;
}

export function OppgaverPage() {
  const [filterOpen, setFilterOpen] = useOpenFilterWhenThreshold(1450);
  const [, setTagsHeight] = useState(0);

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

  const [sorting, setSorting] = useState<OppgaverSorting>("nyeste");

  const oppgaver = useOppgaver(filter.values);
  const sortedOppgaver = sort(oppgaver.data || [], sorting);

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
          <VStack gap="2" className="mr-2">
            <HStack justify="end">
              <Select
                label={"Sortering"}
                onChange={(e) => {
                  setSorting(e.target.value as OppgaverSorting);
                }}
              >
                <option value="nyeste">Nyeste</option>
                <option value="eldste">Eldste</option>
              </Select>
            </HStack>
            <VStack gap="2">
              {sortedOppgaver.map((o) => {
                return <Oppgave key={o.id} oppgave={o} />;
              })}
              {sortedOppgaver.length === 0 && (
                <EmptyState tittel={"Du har ingen nye oppgaver"} beskrivelse={""} />
              )}
            </VStack>
          </VStack>
        }
        filterOpen={filterOpen}
        setFilterOpen={setFilterOpen}
      />
    </ContentBox>
  );
}
