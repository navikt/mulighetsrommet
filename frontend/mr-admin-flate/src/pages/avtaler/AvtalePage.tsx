import { Header } from "@/components/detaljside/Header";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { Heading, HStack, Tabs } from "@navikt/ds-react";
import { useLocation, useMatch } from "react-router";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { AvtaleStatusMedAarsakTag } from "@/components/statuselementer/AvtaleStatusMedAarsakTag";
import { RedaksjoneltInnholdPreview } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdPreview";
import { AvtaleDetaljer } from "./AvtaleDetaljer";
import { AvtalePersonvern } from "./AvtalePersonvern";
import { GjennomforingerForAvtalePage } from "../gjennomforing/GjennomforingerForAvtalePage";
import { RedigerAvtaleContainer } from "@/components/avtaler/RedigerAvtaleContainer";
import { AvtaleDetaljerForm } from "@/components/avtaler/AvtaleDetaljerForm";
import { AvtalePageLayout } from "./AvtalePageLayout";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { AvtalePersonvernForm } from "@/components/avtaler/AvtalePersonvernForm";
import { AvtaleInformasjonForVeiledereForm } from "@/components/avtaler/AvtaleInformasjonForVeiledereForm";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";

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
  const { pathname } = useLocation();
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();
  const { data: avtale } = useAvtale(avtaleId);
  const currentTab = getCurrentTab(pathname);

  const redigeringsmodus = location.pathname.includes("skjema");

  const brodsmuler = useAvtaleBrodsmuler(avtale.id);

  return (
    <div data-testid="avtale_info-container">
      <title>{`Avtale | ${avtale.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <HStack gap="6" className={redigeringsmodus ? "pb-2" : ""}>
          <AvtaleIkon />
          <Heading size="large" level="2">
            {avtale.navn}
          </Heading>
          <AvtaleStatusMedAarsakTag status={avtale.status} />
        </HStack>
      </Header>
      <Tabs value={currentTab}>
        <Tabs.List hidden={redigeringsmodus}>
          {getTabLinks(avtale.id).map(({ label, value, href, testId }) => (
            <Tabs.Tab
              hidden={redigeringsmodus && value !== currentTab}
              key={value}
              label={label}
              value={value}
              onClick={() => navigateAndReplaceUrl(href)}
              data-testid={testId}
            />
          ))}
        </Tabs.List>
        <Tabs.Panel value={AvtaleTab.DETALJER}>
          {redigeringsmodus ? (
            <RedigerAvtaleContainer avtale={avtale}>
              <AvtaleDetaljerForm opsjonerRegistrert={avtale.opsjonerRegistrert} />
            </RedigerAvtaleContainer>
          ) : (
            <AvtalePageLayout avtale={avtale}>
              <AvtaleDetaljer />
            </AvtalePageLayout>
          )}
        </Tabs.Panel>
        <Tabs.Panel value={AvtaleTab.PERSONVERN}>
          {redigeringsmodus ? (
            <RedigerAvtaleContainer avtale={avtale}>
              <AvtalePersonvernForm />
            </RedigerAvtaleContainer>
          ) : (
            <AvtalePageLayout avtale={avtale}>
              <AvtalePersonvern />
            </AvtalePageLayout>
          )}
        </Tabs.Panel>
        <Tabs.Panel value={AvtaleTab.VEILEDERINFORMASJON}>
          {redigeringsmodus ? (
            <RedigerAvtaleContainer avtale={avtale}>
              <AvtaleInformasjonForVeiledereForm />
            </RedigerAvtaleContainer>
          ) : (
            <AvtalePageLayout avtale={avtale}>
              <RedaksjoneltInnholdPreview />
            </AvtalePageLayout>
          )}
        </Tabs.Panel>

        <Tabs.Panel value={AvtaleTab.GJENNOMFORINGER}>
          <InlineErrorBoundary>
            <GjennomforingerForAvtalePage />
          </InlineErrorBoundary>
        </Tabs.Panel>
      </Tabs>
    </div>
  );
}
