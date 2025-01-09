import { DupliserAvtale } from "@/components/avtaler/DupliserAvtale";
import { Header } from "@/components/detaljside/Header";
import headerStyles from "@/components/detaljside/Header.module.scss";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { AvtalestatusTag } from "@/components/statuselementer/AvtalestatusTag";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";
import { useTitle } from "@mr/frontend-common";
import { Alert, Heading, Tabs, VStack } from "@navikt/ds-react";
import { Link, Outlet, useLocation, useMatch } from "react-router";
import { useAvtale } from "../../api/avtaler/useAvtale";
import commonStyles from "../Page.module.scss";
import { Laster } from "../../components/laster/Laster";
import { ContentBox } from "@/layouts/ContentBox";

function useAvtaleBrodsmuler(avtaleId?: string): Array<Brodsmule | undefined> {
  const erPaaGjennomforingerForAvtale = useMatch("/avtaler/:avtaleId/tiltaksgjennomforinger");
  return [
    { tittel: "Avtaler", lenke: "/avtaler" },
    { tittel: "Avtale", lenke: `/avtaler/${avtaleId}` },
    erPaaGjennomforingerForAvtale
      ? {
          tittel: "Gjennomføringer",
          lenke: `/avtaler/${avtaleId}/tiltaksgjennomforinger`,
        }
      : undefined,
  ];
}

export function AvtalePage() {
  const { pathname } = useLocation();
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();
  const { data: avtale, isPending } = useAvtale();

  const brodsmuler = useAvtaleBrodsmuler(avtale?.id);
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
    if (pathname.includes("tiltaksgjennomforinger")) {
      return "tiltaksgjennomforinger";
    } else {
      return "avtale";
    }
  };

  return (
    <>
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
        <ContentBox>
          <div id="panel">
            <Outlet />
          </div>
        </ContentBox>
      </Tabs>
    </>
  );
}
