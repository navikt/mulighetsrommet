import { Alert, Heading, Tabs } from "@navikt/ds-react";
import { Toggles } from "mulighetsrommet-api-client";
import { useTitle } from "mulighetsrommet-frontend-common";
import { Link, Outlet, useLocation } from "react-router-dom";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { Header } from "../../components/detaljside/Header";
import headerStyles from "../../components/detaljside/Header.module.scss";
import { Laster } from "../../components/laster/Laster";
import { AvtalestatusTag } from "../../components/statuselementer/AvtalestatusTag";
import { useGetAvtaleIdFromUrlOrThrow } from "../../hooks/useGetAvtaleIdFromUrl";
import { useNavigateAndReplaceUrl } from "../../hooks/useNavigateWithoutReplacingUrl";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import commonStyles from "../Page.module.scss";
import styles from "./DetaljerAvtalePage.module.scss";
import { DupliserAvtale } from "../../components/avtaler/DupliserAvtale";

export function AvtalePage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { pathname } = useLocation();
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();
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
        <div className={headerStyles.tiltaksnavn_status}>
          <Heading size="large" level="2">
            {avtale.navn ?? "..."}
          </Heading>
          <AvtalestatusTag avtale={avtale} />
          <DupliserAvtale avtale={avtale} />
        </div>
      </Header>
      <Tabs value={currentTab()}>
        <Tabs.List className={commonStyles.list}>
          <Tabs.Tab
            value="info"
            label="Avtaleinfo"
            onClick={() => navigateAndReplaceUrl(`/avtaler/${avtaleId}`)}
            aria-controls="panel"
          />
          {showNotater && (
            <Tabs.Tab
              value="notater"
              label="Notater"
              onClick={() => navigateAndReplaceUrl(`/avtaler/${avtaleId}/notater`)}
              aria-controls="panel"
              data-testid="notater-tab"
            />
          )}
          <Tabs.Tab
            value="tiltaksgjennomforinger"
            label="Gjennomføringer"
            onClick={() => navigateAndReplaceUrl(`/avtaler/${avtaleId}/tiltaksgjennomforinger`)}
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
