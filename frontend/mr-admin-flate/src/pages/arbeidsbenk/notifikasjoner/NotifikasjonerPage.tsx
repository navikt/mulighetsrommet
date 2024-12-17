import { ContainerLayout } from "@/layouts/ContainerLayout";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { useTitle } from "@mr/frontend-common";
import { BellDotFillIcon } from "@navikt/aksel-icons";
import { Tabs } from "@navikt/ds-react";
import { Outlet, useLoaderData, useLocation, useNavigate } from "react-router-dom";
import styles from "../../Page.module.scss";
import arbeidsbenkStyles from "../arbeidsbenk.module.scss";
import { notifikasjonLoader } from "./notifikasjonerLoader";

export function NotifikasjonerPage() {
  const { pathname } = useLocation();
  const { leste, uleste } = useLoaderData<typeof notifikasjonLoader>();
  const navigate = useNavigate();
  useTitle("Notifikasjoner");

  return (
    <main>
      <div className={arbeidsbenkStyles.header}>
        <Tabs value={pathname.includes("tidligere") ? "tidligere" : "nye"} selectionFollowsFocus>
          <Tabs.List id="fane_liste" className={styles.list}>
            <Tabs.Tab
              value="nye"
              label={`Nye notifikasjoner ${uleste?.pagination.totalCount ? `(${uleste?.pagination.totalCount})` : ""}`}
              onClick={() => navigate("/arbeidsbenk/notifikasjoner")}
              aria-controls="panel"
            />
            <Tabs.Tab
              value="tidligere"
              label={`Tidligere notifikasjoner ${leste?.pagination.totalCount ? `(${leste?.pagination.totalCount})` : ""}`}
              onClick={() => navigate("/arbeidsbenk/notifikasjoner/tidligere")}
              aria-controls="panel"
            />
          </Tabs.List>
        </Tabs>
      </div>
      <ContainerLayout>
        <div id="panel">
          <Outlet />
        </div>
      </ContainerLayout>
    </main>
  );
}
