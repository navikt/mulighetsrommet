import { ContainerLayout } from "@/layouts/ContainerLayout";
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

export function OppgaverPage() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  useTitle("Nye oppgaver");

  const oppgaver = useLoaderData<typeof oppgaverLoader>();
  console.log(oppgaver);

  return (
    <main className={oppgaverPageStyles.root}>
      <OppgaverFilter filterAtom={oppgaverFilterAtom} />
      <div className={oppgaverPageStyles.content}>
        <div className={arbeidsbenkStyles.header}>
          <div className={oppgaverPageStyles.sorting}>
            Sortering
            <Select label={"Sortering"} hideLabel>
              <option value="korteste-frist">Korteste Frist</option>
              <option value="korteste-frist">Nyeste</option>
              <option value="korteste-frist">Eldste</option>
            </Select>
          </div>
        </div>
        <div className={oppgaverPageStyles.oppgaver}>
          {oppgaver.map((o) => {
            return <Oppgave oppgave={o} />;
          })}
        </div>
      </div>
    </main>
  );
}
