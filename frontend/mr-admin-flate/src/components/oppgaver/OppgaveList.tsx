import { ToolbarContainer } from "@mr/frontend-common/components/toolbar/toolbarContainer/ToolbarContainer";
import { ToolbarMeny } from "@mr/frontend-common/components/toolbar/toolbarMeny/ToolbarMeny";
import { BodyShort, Select, VStack } from "@navikt/ds-react";
import { Oppgave } from "./Oppgave";
import { EmptyState } from "../notifikasjoner/EmptyState";
import { useState } from "react";
import { useOppgaver } from "@/api/oppgaver/useOppgaver";
import { useSavedFiltersState } from "@/filter/useSavedFiltersState";
import { oppgaverFilterStateAtom } from "@/pages/oppgaveoversikt/oppgaver/filter";
import { GetOppgaverResponse, LagretFilterType } from "@tiltaksadministrasjon/api-client";

type OppgaverSorting = "nyeste" | "eldste";

function sort(oppgaver: GetOppgaverResponse, sorting: OppgaverSorting) {
  return oppgaver.sort((a, b) => {
    const aDate = new Date(a.createdAt);
    const bDate = new Date(b.createdAt);

    return sorting === "nyeste"
      ? bDate.getTime() - aDate.getTime()
      : aDate.getTime() - bDate.getTime();
  });
}

interface Props {
  tagsHeight: number;
  filterOpen: boolean;
}

export default function OppgaverList({ tagsHeight, filterOpen }: Props) {
  const [sorting, setSorting] = useState<OppgaverSorting>("nyeste");

  const { filter } = useSavedFiltersState(oppgaverFilterStateAtom, LagretFilterType.OPPGAVE);

  const oppgaver = useOppgaver(filter.values);
  const sortedOppgaver = sort(oppgaver.data, sorting);

  return (
    <>
      <ToolbarContainer tagsHeight={tagsHeight} filterOpen={filterOpen}>
        <ToolbarMeny>
          <BodyShort weight="semibold">Viser {sortedOppgaver.length} oppgaver</BodyShort>
          <Select
            size="small"
            label={"Sortering"}
            onChange={(e) => {
              setSorting(e.target.value as OppgaverSorting);
            }}
          >
            <option value="nyeste">Nyeste</option>
            <option value="eldste">Eldste</option>
          </Select>
        </ToolbarMeny>
      </ToolbarContainer>
      <VStack gap="2">
        {sortedOppgaver.map((o) => {
          return <Oppgave key={o.id} oppgave={o} />;
        })}
        {sortedOppgaver.length === 0 && (
          <EmptyState tittel={"Du har ingen nye oppgaver"} beskrivelse={""} />
        )}
      </VStack>
    </>
  );
}
