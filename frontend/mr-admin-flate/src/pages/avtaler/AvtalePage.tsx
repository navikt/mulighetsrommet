import { DupliserAvtale } from "@/components/avtaler/DupliserAvtale";
import { Header } from "@/components/detaljside/Header";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { AvtalestatusTag } from "@/components/statuselementer/AvtalestatusTag";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";
import { useTitle } from "@mr/frontend-common";
import { Alert, Heading, Tabs, VStack } from "@navikt/ds-react";
import { Link, Outlet, useLocation, useMatch } from "react-router";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { Laster } from "../../components/laster/Laster";
import { ContentBox } from "@/layouts/ContentBox";
import React from "react";

function useAvtaleBrodsmuler(avtaleId?: string): Array<Brodsmule | undefined> {
  const match = useMatch("/avtaler/:avtaleId/gjennomforinger");
  return [
    { tittel: "Avtaler", lenke: "/avtaler" },
    { tittel: "Avtale", lenke: match ? `/avtaler/${avtaleId}` : undefined },
    match ? { tittel: "Gjennomføringer" } : undefined,
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
    if (pathname.includes("gjennomforinger")) {
      return "gjennomforinger";
    } else {
      return "avtale";
    }
  };

  return (
    <>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <div className="flex justify-start gap-6 items-center flex-wrap">
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
        <Tabs.List className="p-[0 0.5rem] w-[1920px] flex items-start m-auto">
          <Tabs.Tab
            value="avtale"
            label="Avtale"
            onClick={() => navigateAndReplaceUrl(`/avtaler/${avtale.id}`)}
            aria-controls="panel"
          />
          <Tabs.Tab
            value="gjennomforinger"
            label="Gjennomføringer"
            onClick={() => navigateAndReplaceUrl(`/avtaler/${avtale.id}/gjennomforinger`)}
            aria-controls="panel"
            data-testid="gjennomforinger-tab"
          />
        </Tabs.List>
        <React.Suspense fallback={<Laster tekst="Laster innhold..." />}>
          <ContentBox>
            <div id="panel">
              <Outlet />
            </div>
          </ContentBox>
        </React.Suspense>
      </Tabs>
    </>
  );
}
