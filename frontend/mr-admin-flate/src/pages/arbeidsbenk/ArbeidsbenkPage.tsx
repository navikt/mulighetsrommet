import { HeaderBanner } from "@/layouts/HeaderBanner";
import { useTitle } from "@mr/frontend-common";
import { BellDotFillIcon } from "@navikt/aksel-icons";
import { Tabs } from "@navikt/ds-react";
import { Outlet, useLoaderData, useLocation, useNavigate } from "react-router";
import styles from "../Page.module.scss";
import arbeidsbenkStyles from "./arbeidsbenk.module.scss";
import { arbeidsbenkLoader } from "@/pages/arbeidsbenk/arbeidsbenkLoader";
import { ContentBox } from "@/layouts/ContentBox";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";
import { Toggles } from "@mr/api-client";

export function ArbeidsbenkPage() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const { notifikasjoner, enableArbeidsbenk } = useLoaderData<typeof arbeidsbenkLoader>();

  console.log("feature", enableArbeidsbenk);
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
          {enableArbeidsbenk && (
            <Tabs.Tab
              value="oppgaver"
              label={`Oppgaver`}
              onClick={() => navigate("/arbeidsbenk/oppgaver")}
              aria-controls="panel"
            />
          )}
          <Tabs.Tab
            value="notifikasjoner"
            label={`Notifikasjoner ${notifikasjoner ? `(${notifikasjoner})` : ""}`}
            onClick={() => navigate("/arbeidsbenk/notifikasjoner")}
            aria-controls="panel"
            data-testid="notifikasjoner"
          />
        </Tabs.List>
        <ContentBox>
          <div className={arbeidsbenkStyles.container}>
            <Outlet />
          </div>
        </ContentBox>
      </Tabs>
    </main>
  );
}
