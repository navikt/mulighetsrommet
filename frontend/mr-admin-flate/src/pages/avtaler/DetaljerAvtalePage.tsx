import { Alert, Tabs } from "@navikt/ds-react";
import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { Header } from "../../components/detaljside/Header";
import { Laster } from "../../components/laster/Laster";
import { AvtalestatusTag } from "../../components/statuselementer/AvtalestatusTag";
import { useGetAvtaleIdFromUrl } from "../../hooks/useGetAvtaleIdFromUrl";
import commonStyles from "../Page.module.scss";
import styles from "./DetaljerAvtalePage.module.scss";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { useTitle } from "mulighetsrommet-frontend-common";

export function DetaljerAvtalePage() {
  const avtaleId = useGetAvtaleIdFromUrl();
  const { pathname } = useLocation();
  const navigate = useNavigate();
  if (!avtaleId) {
    throw new Error("Fant ingen avtaleId i url");
  }
  const { data: avtale, isLoading } = useAvtale();
  useTitle(`Avtale ${avtale?.navn ? `- ${avtale.navn}` : ""}`);

  if (!avtale && isLoading) {
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
        <div className={commonStyles.header}>
          <span>{avtale?.navn ?? "..."}</span>
          <AvtalestatusTag avtale={avtale} />
        </div>
      </Header>
      <Tabs value={currentTab()}>
        <Tabs.List className={commonStyles.list}>
          <Tabs.Tab
            value="info"
            label="Avtaleinfo"
            onClick={() => navigate(`/avtaler/${avtaleId}`)}
            aria-controls="panel"
          />
          <Tabs.Tab
            value="notater"
            label="Notater"
            onClick={() => navigate(`/avtaler/${avtaleId}/notater`)}
            aria-controls="panel"
            data-testid="notater-tab"
          />
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
