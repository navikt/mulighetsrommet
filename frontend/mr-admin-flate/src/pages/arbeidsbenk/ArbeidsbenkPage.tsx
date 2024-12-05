import { ContainerLayout } from "@/layouts/ContainerLayout";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { useTitle } from "@mr/frontend-common";
import { BellDotFillIcon } from "@navikt/aksel-icons";
import { Tabs } from "@navikt/ds-react";
import { Outlet, useLoaderData, useLocation, useNavigate } from "react-router-dom";
import styles from "../Page.module.scss";

export function ArbeidsbenkPage() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  useTitle("Arbeidsbenk");

  return (
    <main>
      <HeaderBanner
        heading="Arbeidsbenk"
        harUndermeny
        ikon={<BellDotFillIcon title="Arbeidsbenk" className={styles.ikon} />}
      />
      <Tabs
        value={pathname.includes("notifikasjoner") ? "notifikasjoner" : "oppgaver"}
        selectionFollowsFocus
      >
        <Tabs.List id="fane_liste" className={styles.list}>
          <Tabs.Tab
            value="oppgaver"
            label={`Oppgaver (3)`}
            onClick={() => navigate("/arbeidsbenk/oppgaver")}
            aria-controls="panel"
          />
          <Tabs.Tab
            value="notifikasjoner"
            label={`Notifikasjoner (5)`}
            onClick={() => navigate("/arbeidsbenk/notifikasjoner")}
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
