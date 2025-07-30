import { Header } from "@/components/detaljside/Header";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { Heading, HStack, Tabs } from "@navikt/ds-react";
import { useLocation, useMatch } from "react-router";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { AvtaleStatusMedAarsakTag } from "@/components/statuselementer/AvtaleStatusMedAarsakTag";
import { avtaleDetaljerTabAtom } from "@/api/atoms";
import { useAtom } from "jotai";
import { RedaksjoneltInnholdPreview } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdPreview";
import { AvtaleDetaljer } from "./AvtaleDetaljer";
import { AvtalePersonvern } from "./AvtalePersonvern";
import { GjennomforingerForAvtalePage } from "../gjennomforing/GjennomforingerForAvtalePage";
import { RedigerAvtaleContainer } from "@/components/avtaler/RedigerAvtaleContainer";
import { AvtaleDetaljerForm } from "@/components/avtaler/AvtaleDetaljerForm";
import { AvtalePageLayout } from "./AvtalePageLayout";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { AvtaleRedaksjoneltInnholdForm } from "@/components/avtaler/AvtaleRedaksjoneltInnholdForm";
import { AvtalePersonvernForm } from "@/components/avtaler/AvtalePersonvernForm";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { ContentBox } from "@/layouts/ContentBox";

function useAvtaleBrodsmuler(avtaleId?: string): Array<Brodsmule | undefined> {
  const match = useMatch("/avtaler/:avtaleId/gjennomforinger");
  return [
    { tittel: "Avtaler", lenke: "/avtaler" },
    { tittel: "Avtale", lenke: match ? `/avtaler/${avtaleId}` : undefined },
    match ? { tittel: "Gjennomføringer" } : undefined,
  ];
}

export function AvtalePage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const location = useLocation();
  const { data: avtale } = useAvtale(avtaleId);

  const brodsmuler = useAvtaleBrodsmuler(avtale.id);

  const [activeTab, setActiveTab] = useAtom(avtaleDetaljerTabAtom);

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
      <Tabs value={activeTab}>
        <Tabs.List>
          <Tabs.Tab label="Detaljer" value="detaljer" onClick={() => setActiveTab("detaljer")} />
          <Tabs.Tab
            label="Personvern"
            value="personvern"
            onClick={() => setActiveTab("personvern")}
          />
          <Tabs.Tab
            label="Redaksjonelt innhold"
            value="redaksjonelt-innhold"
            onClick={() => setActiveTab("redaksjonelt-innhold")}
          />
          <Tabs.Tab
            value="gjennomforinger"
            label="Gjennomføringer"
            onClick={() => setActiveTab("gjennomforinger")}
            aria-controls="panel"
            data-testid="gjennomforinger-tab"
          />
        </Tabs.List>
        <ContentBox>
          <WhitePaddedBox>
            <Tabs.Panel value="detaljer">
              {location.pathname.includes("skjema") ? (
                <RedigerAvtaleContainer avtale={avtale}>
                  <AvtaleDetaljerForm
                    opsjonerRegistrert={avtale.opsjonerRegistrert}
                    avtalenummer={avtale.avtalenummer}
                  />
                </RedigerAvtaleContainer>
              ) : (
                <AvtalePageLayout avtale={avtale}>
                  <AvtaleDetaljer avtale={avtale} />
                </AvtalePageLayout>
              )}
            </Tabs.Panel>
            <Tabs.Panel value="personvern">
              {location.pathname.includes("skjema") ? (
                <RedigerAvtaleContainer avtale={avtale}>
                  <AvtalePersonvernForm />
                </RedigerAvtaleContainer>
              ) : (
                <AvtalePageLayout avtale={avtale}>
                  <AvtalePersonvern avtale={avtale} />
                </AvtalePageLayout>
              )}
            </Tabs.Panel>
            <Tabs.Panel value="redaksjonelt-innhold">
              {location.pathname.includes("skjema") ? (
                <RedigerAvtaleContainer avtale={avtale}>
                  <AvtaleRedaksjoneltInnholdForm />
                </RedigerAvtaleContainer>
              ) : (
                <AvtalePageLayout avtale={avtale}>
                  <RedaksjoneltInnholdPreview
                    tiltakstype={avtale.tiltakstype}
                    beskrivelse={avtale.beskrivelse}
                    faneinnhold={avtale.faneinnhold}
                    kontorstruktur={avtale.kontorstruktur}
                  />
                </AvtalePageLayout>
              )}
            </Tabs.Panel>
          </WhitePaddedBox>
        </ContentBox>
        <Tabs.Panel value="gjennomforinger">
          <InlineErrorBoundary>
            <GjennomforingerForAvtalePage />
          </InlineErrorBoundary>
        </Tabs.Panel>
      </Tabs>
    </div>
  );
}
