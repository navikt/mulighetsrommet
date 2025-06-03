import { Header } from "@/components/detaljside/Header";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";
import { ContentBox } from "@/layouts/ContentBox";
import { Heading, Tabs, VStack } from "@navikt/ds-react";
import React from "react";
import { Outlet, useLocation, useMatch } from "react-router";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { Laster } from "@/components/laster/Laster";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { AvtaleStatusMedAarsakTag } from "@/components/statuselementer/AvtaleStatusMedAarsakTag";

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
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { data: avtale } = useAvtale(avtaleId);

  const brodsmuler = useAvtaleBrodsmuler(avtale.id);

  const currentTab = () => {
    if (pathname.includes("gjennomforinger")) {
      return "gjennomforinger";
    } else {
      return "avtale";
    }
  };

  return (
    <main>
      <title>{`Avtale | ${avtale.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <div className="flex justify-start gap-6 items-center flex-wrap">
          <AvtaleIkon />
          <VStack>
            <Heading size="large" level="2">
              {avtale.navn}
            </Heading>
          </VStack>
          <AvtaleStatusMedAarsakTag status={avtale.status} />
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
    </main>
  );
}
