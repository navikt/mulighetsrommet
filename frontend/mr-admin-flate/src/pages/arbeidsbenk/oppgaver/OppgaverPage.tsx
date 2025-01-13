import { HeaderBanner } from "@/layouts/HeaderBanner";
import { useTitle } from "@mr/frontend-common";
import { BellDotFillIcon } from "@navikt/aksel-icons";
import { Select, Tabs } from "@navikt/ds-react";
import { Outlet, useLoaderData, useLocation, useNavigate } from "react-router";
import styles from "../../Page.module.scss";
import arbeidsbenkStyles from "../arbeidsbenk.module.scss";
import oppgaverPageStyles from "./OppgaverPage.module.scss";
import { OppgaverFilter } from "../../../components/filter/OppgaverFilter";
import { oppgaverFilterAtom } from "@/api/atoms";
import { oppgaverLoader } from "@/pages/arbeidsbenk/oppgaver/oppgaverLoader";
import { Oppgave } from "@/components/oppgaver/Oppgave";
import { useState } from "react";
import { GetOppgaverResponse } from "@mr/api-client";
import { useAtom } from "jotai/index";

type OppgaverSorting = "korteste-frist" | "nyeste" | "eldste";

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
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const [sorting, setSorting] = useState<OppgaverSorting>("korteste-frist");
  useTitle("Oppgaver");
  const { oppgaver, tiltakstyper } = useLoaderData<typeof oppgaverLoader>();
  const sortedOppgaver = sort(oppgaver, sorting);
  const [filter] = useAtom(oppgaverFilterAtom);

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
            return <Oppgave key={o.createdAt} oppgave={o} />;
          })}
        </div>
      </div>
    </main>
  );
}
