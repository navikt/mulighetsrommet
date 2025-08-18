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
import { AvtalePersonvernForm } from "@/components/avtaler/AvtalePersonvernForm";
import { AvtaleInformasjonTilVeiledereneForm } from "@/components/avtaler/AvtaleInformasjonTilVeiledereneForm";

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

  const redigeringsmodus = location.pathname.includes("skjema");

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
            label="Informasjon til veilederene"
            value="redaksjonelt-innhold"
            onClick={() => setActiveTab("redaksjonelt-innhold")}
          />
          {!redigeringsmodus && (
            <Tabs.Tab
              value="gjennomforinger"
              label="Gjennomføringer"
              onClick={() => setActiveTab("gjennomforinger")}
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
              <AvtaleDetaljer avtale={avtale} />
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
              <AvtalePersonvern avtale={avtale} />
            </AvtalePageLayout>
          )}
        </Tabs.Panel>
        <Tabs.Panel value="redaksjonelt-innhold">
          {redigeringsmodus ? (
            <RedigerAvtaleContainer avtale={avtale}>
              <AvtaleInformasjonTilVeiledereneForm />
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

        <Tabs.Panel value="gjennomforinger">
          <InlineErrorBoundary>
            <GjennomforingerForAvtalePage />
          </InlineErrorBoundary>
        </Tabs.Panel>
      </Tabs>
    </div>
  );
}
