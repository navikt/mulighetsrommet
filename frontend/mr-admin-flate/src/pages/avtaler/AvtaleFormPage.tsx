import { useAvtale } from "@/api/avtaler/useAvtale";
import { AvtaleDetaljerForm } from "@/components/avtaler/AvtaleDetaljerForm";
import { AvtaleFormKnapperad } from "@/components/avtaler/AvtaleFormKnapperad";
import { AvtaleInformasjonForVeiledereForm } from "@/components/avtaler/AvtaleInformasjonForVeiledereForm";
import { AvtalePersonvernForm } from "@/components/avtaler/AvtalePersonvernForm";
import { RedigerAvtaleContainer } from "@/components/avtaler/RedigerAvtaleContainer";
import { Header } from "@/components/detaljside/Header";
import { Separator } from "@/components/detaljside/Metadata";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { AvtaleStatusMedAarsakTag } from "@/components/statuselementer/AvtaleStatusMedAarsakTag";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { useNavigateAndReplaceUrl } from "@/hooks/useNavigateWithoutReplacingUrl";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Heading, Tabs } from "@navikt/ds-react";
import { useLocation } from "react-router";

function brodsmuler(avtaleId: string): Array<Brodsmule | undefined> {
  return [
    {
      tittel: "Avtaler",
      lenke: "/avtaler",
    },
    {
      tittel: "Avtale",
      lenke: `/avtaler/${avtaleId}`,
    },
    {
      tittel: "Rediger avtale",
    },
  ];
}

enum AvtaleTab {
  DETALJER = "detaljer",
  PERSONVERN = "personvern",
  VEILEDERINFORMASJON = "veilederinformasjon",
}

function getCurrentTab(pathname: string) {
  if (pathname.includes("veilederinformasjon")) {
    return AvtaleTab.VEILEDERINFORMASJON;
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
      href: `/avtaler/${avtaleId}/skjema`,
    },
    {
      label: "Personvern",
      value: AvtaleTab.PERSONVERN,
      href: `/avtaler/${avtaleId}/personvern/skjema`,
    },
    {
      label: "Informasjon for veiledere",
      value: AvtaleTab.VEILEDERINFORMASJON,
      href: `/avtaler/${avtaleId}/veilederinformasjon/skjema`,
    },
  ];
}

export function AvtaleFormPage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const { pathname } = useLocation();
  const { navigateAndReplaceUrl } = useNavigateAndReplaceUrl();
  const { data: avtale } = useAvtale(avtaleId);
  const currentTab = getCurrentTab(pathname);
  return (
    <div data-testid="avtale-form-page">
      <title>{`Redigerer avtale | ${avtale.navn}`}</title>
      <Brodsmuler brodsmuler={brodsmuler(avtaleId)} />
      <Header>
        <AvtaleIkon />
        <Heading size="large" level="2">
          Redigerer ${avtale.navn}
        </Heading>
        <AvtaleStatusMedAarsakTag status={avtale.status} />
      </Header>
      <RedigerAvtaleContainer avtale={avtale}>
        <Tabs value={currentTab}>
          <Tabs.List>
            {getTabLinks(avtale.id).map(({ label, value, href, testId }) => (
              <Tabs.Tab
                key={value}
                label={label}
                value={value}
                onClick={() => navigateAndReplaceUrl(href)}
                data-testid={testId}
              />
            ))}
          </Tabs.List>
          <FormTabsPanel value={AvtaleTab.DETALJER}>
            <AvtaleDetaljerForm opsjonerRegistrert={avtale.opsjonerRegistrert} />
          </FormTabsPanel>
          <FormTabsPanel value={AvtaleTab.PERSONVERN}>
            <AvtalePersonvernForm />
          </FormTabsPanel>
          <FormTabsPanel value={AvtaleTab.VEILEDERINFORMASJON}>
            <AvtaleInformasjonForVeiledereForm />
          </FormTabsPanel>
        </Tabs>
      </RedigerAvtaleContainer>
    </div>
  );
}

interface FormTabsPanelProps {
  value: AvtaleTab;
  children: React.ReactNode;
}

function FormTabsPanel({ value, children }: FormTabsPanelProps) {
  return (
    <Tabs.Panel value={value}>
      <ContentBox>
        <WhitePaddedBox>
          <AvtaleFormKnapperad />
          <Separator />
          {children}
          <Separator />
          <AvtaleFormKnapperad />
        </WhitePaddedBox>
      </ContentBox>
    </Tabs.Panel>
  );
}
