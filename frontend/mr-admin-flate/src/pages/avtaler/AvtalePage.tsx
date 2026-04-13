import { Header } from "@/components/detaljside/Header";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { Box, Heading, Tabs } from "@navikt/ds-react";
import { Outlet, useLocation, useMatch } from "react-router";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";
import { DataElementStatusTag } from "@mr/frontend-common";

function useAvtaleBrodsmuler(avtaleId?: string): Array<Brodsmule | undefined> {
  const match = useMatch("/avtaler/:avtaleId/gjennomforinger");
  return [
    { tittel: "Avtaler", lenke: "/avtaler" },
    { tittel: "Avtale", lenke: match ? `/avtaler/${avtaleId}` : undefined },
    match ? { tittel: "Gjennomføringer" } : undefined,
  ];
}

enum AvtaleTab {
  DETALJER = "detaljer",
  PERSONVERN = "personvern",
  VEILEDERINFORMASJON = "veilederinformasjon",
  GJENNOMFORINGER = "gjennomforinger",
}

function getCurrentTab(pathname: string) {
  if (pathname.includes("veilederinformasjon")) {
    return AvtaleTab.VEILEDERINFORMASJON;
  } else if (pathname.includes("gjennomforinger")) {
    return AvtaleTab.GJENNOMFORINGER;
  } else if (pathname.includes("personvern")) {
    return AvtaleTab.PERSONVERN;
  } else {
    return AvtaleTab.DETALJER;
  }
}

interface AvtaleTabDetaljer {
  label: string;
  value: AvtaleTab;
  href: string;
  testId?: string;
}

function getTabLinks(avtaleId: string): AvtaleTabDetaljer[] {
  return [
    {
      label: "Detaljer",
      value: AvtaleTab.DETALJER,
      href: `/avtaler/${avtaleId}`,
    },
    {
      label: "Personvern",
      value: AvtaleTab.PERSONVERN,
      href: `/avtaler/${avtaleId}/personvern`,
    },
    {
      label: "Informasjon for veiledere",
      value: AvtaleTab.VEILEDERINFORMASJON,
      href: `/avtaler/${avtaleId}/veilederinformasjon`,
    },
    {
      label: "Gjennomføringer",
      value: AvtaleTab.GJENNOMFORINGER,
      href: `/avtaler/${avtaleId}/gjennomforinger`,
      testId: "gjennomforinger-tab",
    },
  ];
}

export function AvtalePage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { data: avtale } = useAvtale(avtaleId);
  const { pathname } = useLocation();
  const currentTab = getCurrentTab(pathname);
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();

  const brodsmuler = useAvtaleBrodsmuler(avtale.id);

  return (
    <div data-testid="avtale_info-container">
      <title>{`Avtale | ${avtale.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <AvtaleIkon />
        <Heading size="large" level="2">
          {avtale.navn}
        </Heading>
        <DataElementStatusTag {...avtale.status.status} />
      </Header>
      <Tabs value={currentTab}>
        <Box background="default">
          <Tabs.List>
            {getTabLinks(avtale.id).map(({ label, value, href, testId }) => (
              <Tabs.Tab
                key={value}
                label={label}
                value={value}
                onClick={() => {
                  navigateAndReplaceUrl(href);
                }}
                data-testid={testId}
              />
            ))}
          </Tabs.List>
        </Box>
        <Tabs.Panel value={currentTab}>
          <Outlet />
        </Tabs.Panel>
      </Tabs>
    </div>
  );
}
