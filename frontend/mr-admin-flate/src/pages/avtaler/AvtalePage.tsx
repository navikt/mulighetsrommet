import { Alert, Heading, Tabs } from "@navikt/ds-react";
import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { Header } from "../../components/detaljside/Header";
import { Laster } from "../../components/laster/Laster";
import { AvtalestatusTag } from "../../components/statuselementer/AvtalestatusTag";
import { useGetAvtaleIdFromUrlOrThrow } from "../../hooks/useGetAvtaleIdFromUrl";
import commonStyles from "../Page.module.scss";
import styles from "./DetaljerAvtalePage.module.scss";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { useTitle } from "mulighetsrommet-frontend-common";
import { Toggles } from "mulighetsrommet-api-client";
import { useFeatureToggle } from "../../api/features/feature-toggles";

export function AvtalePage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const { data: showNotater } = useFeatureToggle(Toggles.MULIGHETSROMMET_ADMIN_FLATE_SHOW_NOTATER);
  const { data: avtale, isPending } = useAvtale();
  useTitle(`Avtale ${avtale?.navn ? `- ${avtale.navn}` : ""}`);

  if (isPending) {
    return (
      <main>
        <Laster tekst="Laster avtale" />
      </main>
    );
  }

  if (!avtale) {
    return (
      <Alert variant="warning">
        Klarte ikke finne avtale
        <div>
          <Link to="/">Til forside</Link>
        </div>
      </Alert>
    );
  }

  const currentTab = () => {
    if (pathname.includes("notater")) {
      return "notater";
    } else if (pathname.includes("tiltaksgjennomforinger")) {
      return "tiltaksgjennomforinger";
    } else {
      return "info";
    }
  };

  return (
    <main className={styles.avtaleinfo}>
      <Header>
        <Heading size="large" level="2">
          {avtale.navn ?? "..."}
        </Heading>
        <AvtalestatusTag avtale={avtale} />
      </Header>
      <Tabs value={currentTab()}>
        <Tabs.List className={commonStyles.list}>
          <Tabs.Tab
            value="info"
            label="Avtaleinfo"
            onClick={() => navigate(`/avtaler/${avtaleId}`)}
            aria-controls="panel"
          />
          {showNotater && (
            <Tabs.Tab
              value="notater"
              label="Notater"
              onClick={() => navigate(`/avtaler/${avtaleId}/notater`)}
              aria-controls="panel"
              data-testid="notater-tab"
            />
          )}
          <Tabs.Tab
            value="tiltaksgjennomforinger"
            label="Gjennomføringer"
            onClick={() => navigate(`/avtaler/${avtaleId}/tiltaksgjennomforinger`)}
            aria-controls="panel"
            data-testid="gjennomforinger-tab"
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
