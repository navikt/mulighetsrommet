import { Header } from "@/components/detaljside/Header";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { Heading, Tabs } from "@navikt/ds-react";
import { useLocation, useMatch } from "react-router";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { RedaksjoneltInnholdPreview } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdPreview";
import { AvtaleDetaljer } from "./AvtaleDetaljer";
import { AvtalePersonvern } from "./AvtalePersonvern";
import { GjennomforingerForAvtalePage } from "../gjennomforing/GjennomforingerForAvtalePage";
import { AvtalePageLayout } from "./AvtalePageLayout";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";
import { DataElementStatusTag } from "@mr/frontend-common";
import { useUmami } from "@/sporing/useUmami";
import { AvtaleDto } from "@tiltaksadministrasjon/api-client";

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

function getTab(currentTab: AvtaleTab, avtale: AvtaleDto) {
  switch (currentTab) {
    case AvtaleTab.DETALJER:
      return (
        <AvtalePageLayout avtale={avtale}>
          <AvtaleDetaljer />
        </AvtalePageLayout>
      );
    case AvtaleTab.PERSONVERN:
      return (
        <AvtalePageLayout avtale={avtale}>
          <AvtalePersonvern />
        </AvtalePageLayout>
      );
    case AvtaleTab.VEILEDERINFORMASJON:
      return (
        <AvtalePageLayout avtale={avtale}>
          <RedaksjoneltInnholdPreview />
        </AvtalePageLayout>
      );
    case AvtaleTab.GJENNOMFORINGER:
      return (
        <InlineErrorBoundary>
          <GjennomforingerForAvtalePage />
        </InlineErrorBoundary>
      );
  }
}

export function AvtalePage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { pathname } = useLocation();
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();
  const { data: avtale } = useAvtale(avtaleId);
  const { logUmamiHendelse } = useUmami();
  const currentTab = getCurrentTab(pathname);

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
        <Tabs.List className="bg-ax-bg-default">
          {getTabLinks(avtale.id).map(({ label, value, href, testId }) => (
            <Tabs.Tab
              key={value}
              label={label}
              value={value}
              onClick={() => {
                logUmamiHendelse({
                  type: "FANE_BYTTET",
                  tekst: label,
                  fraFane: currentTab,
                  sidenavn: "Avtale",
                });
                navigateAndReplaceUrl(href);
              }}
              data-testid={testId}
            />
          ))}
        </Tabs.List>
        <Tabs.Panel value={currentTab}>{getTab(currentTab, avtale)}</Tabs.Panel>
      </Tabs>
    </div>
  );
}
