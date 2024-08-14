import { ContainerLayout } from "@/layouts/ContainerLayout";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { BellDotFillIcon } from "@navikt/aksel-icons";
import { Tabs } from "@navikt/ds-react";
import { useTitle } from "@mr/frontend-common";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import styles from "../Page.module.scss";
import { useNotifikasjonerForAnsatt } from "../../api/notifikasjoner/useNotifikasjonerForAnsatt";
import { NotificationStatus } from "@mr/api-client";

export function NotifikasjonerPage() {
  const { pathname } = useLocation();
  const { data: lesteNotifikasjoner } = useNotifikasjonerForAnsatt(NotificationStatus.DONE);
  const { data: ulesteNotifikasjoner } = useNotifikasjonerForAnsatt(NotificationStatus.NOT_DONE);
  const navigate = useNavigate();
  useTitle("Notifikasjoner");

  return (
    <main>
      <HeaderBanner
        heading="Notifikasjoner"
        harUndermeny
        ikon={<BellDotFillIcon title="Notifikasjoner" className={styles.ikon} />}
      />
      <Tabs value={pathname.includes("tidligere") ? "tidligere" : "nye"} selectionFollowsFocus>
        <Tabs.List id="fane_liste" className={styles.list}>
          <Tabs.Tab
            value="nye"
            label={`Nye notifikasjoner ${ulesteNotifikasjoner?.pagination.totalCount ? `(${ulesteNotifikasjoner?.pagination.totalCount})` : ""}`}
            onClick={() => navigate("/notifikasjoner")}
            aria-controls="panel"
          />
          <Tabs.Tab
            value="tidligere"
            label={`Tidligere notifikasjoner ${lesteNotifikasjoner?.pagination.totalCount ? `(${lesteNotifikasjoner?.pagination.totalCount})` : ""}`}
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
