import { useTitle } from "@mr/frontend-common";
import { Select } from "@navikt/ds-react";
import { useLoaderData } from "react-router";
import arbeidsbenkStyles from "../arbeidsbenk.module.scss";
import oppgaverPageStyles from "./OppgaverPage.module.scss";
import { OppgaverFilter } from "../../../components/filter/OppgaverFilter";
import { oppgaverFilterAtom } from "@/api/atoms";
import { oppgaverLoader } from "@/pages/arbeidsbenk/oppgaver/oppgaverLoader";
import { Oppgave } from "@/components/oppgaver/Oppgave";
import { useState } from "react";
import { GetOppgaverResponse } from "@mr/api-client";
import { useAtom } from "jotai/index";
import { useOppgaver } from "@/api/oppgaver/useOppgaver";

type OppgaverSorting = "korteste-frist" | "nyeste" | "eldste";
// @TODO: Should maybe be on the backend?
function sort(oppgaver: GetOppgaverResponse, sorting: OppgaverSorting) {
  if (sorting === "korteste-frist") {
    return oppgaver.sort((a, b) => {
      const aDate = new Date(a.frist);
      const bDate = new Date(b.frist);

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
  const [sorting, setSorting] = useState<OppgaverSorting>("korteste-frist");
  useTitle("Oppgaver");
  const [filter] = useAtom(oppgaverFilterAtom);
  const { tiltakstyper } = useLoaderData<typeof oppgaverLoader>();
  const oppgaver = useOppgaver(filter);
  const sortedOppgaver = sort(oppgaver.data || [], sorting);

  return (
    <main className={oppgaverPageStyles.root}>
      <OppgaverFilter filterAtom={oppgaverFilterAtom} tiltakstyper={tiltakstyper} />
      <div className={oppgaverPageStyles.content}>
        <div className={arbeidsbenkStyles.header}>
          <div className={oppgaverPageStyles.sorting}>
            Sortering
            <Select
              label={"Sortering"}
              hideLabel
              onChange={(e) => {
                setSorting(e.target.value as OppgaverSorting);
                console.log(e.target.value);
              }}
            >
              <option value="korteste-frist">Korteste Frist</option>
              <option value="nyeste">Nyeste</option>
              <option value="eldste">Eldste</option>
            </Select>
          </div>
        </div>
        <div className={oppgaverPageStyles.oppgaver}>
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
        </div>
      </div>
    </main>
  );
}
