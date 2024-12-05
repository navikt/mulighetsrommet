import { useAvtale } from "@/api/avtaler/useAvtale";
import { Alert, Heading, Tabs, VStack } from "@navikt/ds-react";
import { useTitle } from "@mr/frontend-common";
import { Link, Outlet, useLocation, useMatch } from "react-router-dom";
import { DupliserAvtale } from "@/components/avtaler/DupliserAvtale";
import { Header } from "@/components/detaljside/Header";
import headerStyles from "@/components/detaljside/Header.module.scss";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Laster } from "@/components/laster/Laster";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { AvtalestatusTag } from "@/components/statuselementer/AvtalestatusTag";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";
import { ContainerLayout } from "@/layouts/ContainerLayout";
import commonStyles from "../Page.module.scss";
import styles from "./AvtalePage.module.scss";

function useAvtaleBrodsmuler(avtaleId?: string): Array<Brodsmule | undefined> {
  const erPaaGjennomforingerForAvtale = useMatch("/avtaler/:avtaleId/tiltaksgjennomforinger");
  return [
    { tittel: "Forside", lenke: "/" },
    { tittel: "Avtaler", lenke: "/avtaler" },
    { tittel: "Avtaledetaljer", lenke: `/avtaler/${avtaleId}` },
    erPaaGjennomforingerForAvtale
      ? {
          tittel: "Avtalens gjennomføringer",
          lenke: `/avtaler/${avtaleId}/tiltaksgjennomforinger`,
        }
      : undefined,
  ];
}

export function AvtalePage() {
  const { pathname } = useLocation();
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();
  const { data: avtale, isPending } = useAvtale();
  useTitle(`Avtale ${avtale?.navn ? `- ${avtale.navn}` : ""}`);
  const brodsmuler = useAvtaleBrodsmuler(avtale?.id);

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
    if (pathname.includes("tiltaksgjennomforinger")) {
      return "tiltaksgjennomforinger";
    } else {
      return "avtale";
    }
  };

  return (
    <main className={styles.avtaleinfo}>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <div className={headerStyles.tiltaksnavn_status}>
          <AvtaleIkon />
          <VStack>
            <Heading size="large" level="2">
              {avtale.navn ?? "..."}
            </Heading>
          </VStack>
          <AvtalestatusTag avtale={avtale} showAvbruttAarsak />
          <DupliserAvtale avtale={avtale} />
        </div>
      </Header>
      <Tabs value={currentTab()}>
        <Tabs.List className={commonStyles.list}>
          <Tabs.Tab
            value="avtale"
            label="Avtale"
            onClick={() => navigateAndReplaceUrl(`/avtaler/${avtale.id}`)}
            aria-controls="panel"
          />
          <Tabs.Tab
            value="tiltaksgjennomforinger"
            label="Gjennomføringer"
            onClick={() => navigateAndReplaceUrl(`/avtaler/${avtale.id}/tiltaksgjennomforinger`)}
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
