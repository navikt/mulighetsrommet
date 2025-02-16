import { oppgaverFilterAtom } from "@/api/atoms";
import { useOppgaver } from "@/api/oppgaver/useOppgaver";
import { EmptyState } from "@/components/notifikasjoner/EmptyState";
import { Oppgave } from "@/components/oppgaver/Oppgave";
import { oppgaverLoader } from "@/pages/arbeidsbenk/oppgaver/oppgaverLoader";
import { GetOppgaverResponse } from "@mr/api-client-v2";
import { useOpenFilterWhenThreshold, useTitle } from "@mr/frontend-common";
import { FilterAndTableLayout } from "@mr/frontend-common/components/filterAndTableLayout/FilterAndTableLayout";
import { Select } from "@navikt/ds-react";
import { useAtom } from "jotai/index";
import { useState } from "react";
import { useLoaderData } from "react-router";
import { OppgaverFilter } from "../../../components/filter/OppgaverFilter";
import { OppgaveFilterTags } from "../../../components/filter/OppgaverFilterTags";
import { NullstillKnappForOppgaver } from "./NullstillKnappForOppgaver";
import { ContentBox } from "../../../layouts/ContentBox";

type OppgaverSorting = "korteste-frist" | "nyeste" | "eldste";

function sort(oppgaver: GetOppgaverResponse, sorting: OppgaverSorting) {
  if (sorting === "korteste-frist") {
    return oppgaver.sort((a, b) => {
      const aDate = new Date(a.deadline);
      const bDate = new Date(b.deadline);

      return aDate.getTime() - bDate.getTime();
    });
  }
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
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [_, setTagsHeight] = useState(0);
  const [sorting, setSorting] = useState<OppgaverSorting>("korteste-frist");
  useTitle("Oppgaver");
  const [filter] = useAtom(oppgaverFilterAtom);
  const { tiltakstyper, regioner } = useLoaderData<typeof oppgaverLoader>();
  const oppgaver = useOppgaver(filter);
  const sortedOppgaver = sort(oppgaver.data || [], sorting);

  return (
    <ContentBox>
      <FilterAndTableLayout
        filter={
          <OppgaverFilter
            filterAtom={oppgaverFilterAtom}
            tiltakstyper={tiltakstyper}
            regioner={regioner}
          />
        }
        tags={
          <OppgaveFilterTags
            filterAtom={oppgaverFilterAtom}
            filterOpen={filterOpen}
            setTagsHeight={setTagsHeight}
          />
        }
        buttons={null}
        table={
          <div className="flex flex-col">
            <div className="flex justify-end">
              <div>
                <Select
                  label={"Sortering"}
                  onChange={(e) => {
                    setSorting(e.target.value as OppgaverSorting);
                  }}
                >
                  <option value="korteste-frist">Korteste frist</option>
                  <option value="nyeste">Nyeste</option>
                  <option value="eldste">Eldste</option>
                </Select>
              </div>
            </div>
            <div className="grid grid-cols-1 gap-2 mt-4">
              {sortedOppgaver.map((o) => {
                // @TODO: Should maybe have something like tiltakstypeName come from the backend instead of doing manual mapping
                return (
                  <Oppgave
                    key={o.createdAt}
                    tiltakstype={tiltakstyper.find((t) => t.tiltakskode === o.tiltakstype)!}
                    oppgave={o}
                  />
                );
              })}
              {sortedOppgaver.length === 0 && (
                <EmptyState tittel={"Du har ingen nye oppgaver"} beskrivelse={""} />
              )}
            </div>
          </div>
        }
        filterOpen={filterOpen}
        setFilterOpen={setFilterOpen}
        nullstillFilterButton={<NullstillKnappForOppgaver filterAtom={oppgaverFilterAtom} />}
      />
    </ContentBox>
  );
}
