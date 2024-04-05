import { Tabs } from "@navikt/ds-react";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { ContainerLayout } from "@/layouts/ContainerLayout";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import styles from "../Page.module.scss";
import { useTitle } from "mulighetsrommet-frontend-common";
import { BellDotIcon } from "@navikt/aksel-icons";

export function NotifikasjonerPage() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  useTitle("Notifikasjoner");

  return (
    <main>
      <HeaderBanner
        heading="Notifikasjoner"
        harUndermeny
        ikon={<BellDotIcon title="Notifikasjoner" fontSize="2rem" />}
      />
      <Tabs value={pathname.includes("tidligere") ? "tidligere" : "nye"} selectionFollowsFocus>
        <Tabs.List id="fane_liste" className={styles.list}>
          <Tabs.Tab
            value="nye"
            label="Nye notifikasjoner"
            onClick={() => navigate("/notifikasjoner")}
            aria-controls="panel"
          />
          <Tabs.Tab
            value="tidligere"
            label="Tidligere notifikasjoner"
            onClick={() => navigate("/notifikasjoner/tidligere")}
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
