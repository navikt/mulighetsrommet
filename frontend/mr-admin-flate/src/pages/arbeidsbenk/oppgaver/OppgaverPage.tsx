import { ContainerLayout } from "@/layouts/ContainerLayout";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { useTitle } from "@mr/frontend-common";
import { BellDotFillIcon } from "@navikt/aksel-icons";
import { Tabs } from "@navikt/ds-react";
import { Outlet, useLoaderData, useLocation, useNavigate } from "react-router-dom";
import styles from "../../Page.module.scss";

export function OppgaverPage() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  useTitle("Nye oppgaver");

  return (
    <main>
      <HeaderBanner
        heading="Nye oppgaver"
        harUndermeny
        ikon={<BellDotFillIcon title="Notifikasjoner" className={styles.ikon} />}
      />
      <Tabs value={pathname.includes("fullforte") ? "fullforte" : "nye"} selectionFollowsFocus>
        <Tabs.List id="fane_liste" className={styles.list}>
          <Tabs.Tab
            value="nye"
            label={`Nye oppgaver`}
            onClick={() => navigate("/arbeidsbenk/oppgaver")}
            aria-controls="panel"
          />
          <Tabs.Tab
            value="fullforte"
            label={`Fullførte oppgaver`}
            onClick={() => navigate("/arbeidsbenk/oppgaver/fullforte")}
            aria-controls="panel"
          />
        </Tabs.List>
        <ContainerLayout>
          <div id="panel">
            <Outlet />
          </div>
        </ContainerLayout>
      </Tabs>
    </main>
  );
}
