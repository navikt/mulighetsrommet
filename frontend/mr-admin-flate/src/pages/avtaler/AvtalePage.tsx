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

function getCurrentTab(pathname: string) {
  if (pathname.includes("veilederinformasjon")) {
    return "veilederinformasjon";
  } else if (pathname.includes("gjennomforinger")) {
    return "gjennomforinger";
  } else if (pathname.includes("personvern")) {
    return "personvern";
  } else {
    return "detaljer";
  }
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
        <HStack gap="6">
          <AvtaleIkon />
          <Heading size="large" level="2">
            {avtale.navn}
          </Heading>
          <AvtaleStatusMedAarsakTag status={avtale.status} />
        </HStack>
      </Header>
      <Tabs value={currentTab}>
        <Tabs.List>
          <Tabs.Tab
            label="Detaljer"
            value="detaljer"
            onClick={() => navigateAndReplaceUrl(`/avtaler/${avtale.id}`)}
          />
          <Tabs.Tab
            label="Personvern"
            value="personvern"
            onClick={() => navigateAndReplaceUrl(`/avtaler/${avtale.id}/personvern`)}
          />
          <Tabs.Tab
            label="Informasjon for veiledere"
            value="veilederinformasjon"
            onClick={() => navigateAndReplaceUrl(`/avtaler/${avtale.id}/veilederinformasjon`)}
          />
          {!redigeringsmodus && (
            <Tabs.Tab
              value="gjennomforinger"
              label="Gjennomføringer"
              onClick={() => navigateAndReplaceUrl(`/avtaler/${avtale.id}/gjennomforinger`)}
              data-testid="gjennomforinger-tab"
            />
          )}
        </Tabs.List>
        <Tabs.Panel value="detaljer">
          {redigeringsmodus ? (
            <RedigerAvtaleContainer avtale={avtale}>
              <AvtaleDetaljerForm
                opsjonerRegistrert={avtale.opsjonerRegistrert}
                avtalenummer={avtale.avtalenummer}
              />
            </RedigerAvtaleContainer>
          ) : (
            <AvtalePageLayout avtale={avtale}>
              <AvtaleDetaljer />
            </AvtalePageLayout>
          )}
        </Tabs.Panel>
        <Tabs.Panel value="personvern">
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
        <Tabs.Panel value="veilederinformasjon">
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

        <Tabs.Panel value="gjennomforinger">
          <InlineErrorBoundary>
            <GjennomforingerForAvtalePage />
          </InlineErrorBoundary>
        </Tabs.Panel>
      </Tabs>
    </div>
  );
}
