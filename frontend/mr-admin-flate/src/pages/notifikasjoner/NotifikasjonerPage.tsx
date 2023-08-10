import { Heading, Tabs } from "@navikt/ds-react";
import { kebabCase } from "mulighetsrommet-veileder-flate/src/utils/Utils";
import styles from "./NotifikasjonerPage.module.scss";
import { Outlet, useLocation, useNavigate } from "react-router-dom";

export function NotifikasjonerPage() {
  const { pathname } = useLocation();
  const navigate = useNavigate();

  return (
    <main className={styles.notifikasjoner}>
      <Heading level="2" size="large" className={styles.heading}>
        Notifikasjoner
      </Heading>
      <Tabs
        value={pathname.includes("tidligere") ? "tidligere" : "nye"}
        size="small"
        selectionFollowsFocus
        className={styles.fane_root}
      >
        <Tabs.List className={styles.fane_liste} id="fane_liste">
          <Tabs.Tab
            value="nye"
            label="Nye notifikasjoner"
            className={styles.btn_tab}
            data-testid={`fane_${kebabCase("Nye notifikasjoner")}`}
            onClick={() => navigate("/notifikasjoner")}
            aria-controls="panel"
          />
          <Tabs.Tab
            value="tidligere"
            label="Tidligere notifikasjoner"
            className={styles.btn_tab}
            data-testid={`fane_${kebabCase("Tidligere notifikasjoner")}`}
            onClick={() => navigate("/notifikasjoner/tidligere")}
            aria-controls="panel"
          />
        </Tabs.List>
        <div id="panel" className={styles.fane_panel}>
          <Outlet />
        </div>
      </Tabs>
    </main>
  );
}
