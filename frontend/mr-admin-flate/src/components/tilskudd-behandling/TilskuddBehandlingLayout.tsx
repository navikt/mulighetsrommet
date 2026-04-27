import { useEnkeltplassGjennomforingOrError } from "@/api/gjennomforing/useGjennomforing";
import { Header } from "@/components/detaljside/Header";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { DeltakerinformasjonOgBetalingsbetingelser } from "@/components/tilskudd-behandling/DeltakerinformasjonOgBetalingsbetingelser";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { ToTrinnsOpprettelsesForklaring } from "@/pages/gjennomforing/tilsagn/ToTrinnsOpprettelseForklaring";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { GavelSoundBlockFillIcon } from "@navikt/aksel-icons";
import { Box, Heading, Tabs } from "@navikt/ds-react";
import { TotrinnskontrollDto } from "@tiltaksadministrasjon/api-client";
import React from "react";

export type TilskuddBehandlingTab = "saksopplysninger" | "vedtak";

interface Props {
  gjennomforingId: string;
  currentTab: TilskuddBehandlingTab;
  onTabChange: (tab: TilskuddBehandlingTab) => void;
  tabList: React.ReactNode;
  opprettelse?: TotrinnskontrollDto;
  saksopplysningerContent: React.ReactNode;
  vedtakContent: React.ReactNode;
  actions: React.ReactNode;
}

export function TilskuddBehandlingLayout({
  gjennomforingId,
  currentTab,
  opprettelse,
  onTabChange,
  tabList,
  saksopplysningerContent,
  vedtakContent,
  actions,
}: Props) {
  const { gjennomforing, enkeltplassDeltaker } =
    useEnkeltplassGjennomforingOrError(gjennomforingId);

  return (
    <>
      <title>Tilskuddsbehandling</title>
      <Brodsmuler
        brodsmuler={[
          { tittel: "Gjennomføringer", lenke: `/gjennomforinger` },
          { tittel: "Gjennomføring", lenke: `/gjennomforinger/${gjennomforingId}` },
          {
            tittel: "Tilskuddsbehandlinger",
            lenke: `/gjennomforinger/${gjennomforingId}/tilskudd-behandling` as const,
          },
          { tittel: "Behandling" },
        ]}
      />
      <Header>
        <GavelSoundBlockFillIcon
          color="var(--ax-text-brand-blue-decoration)"
          aria-hidden
          width="2.5rem"
          height="2.5rem"
        />
        <Heading size="large" level="2">
          Tilskuddsbehandling
        </Heading>
      </Header>
      <WhitePaddedBox>
        <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
        {opprettelse && (
          <ToTrinnsOpprettelsesForklaring
            heading="Behandlingen ble returnert"
            opprettelse={opprettelse}
          />
        )}
        <Tabs value={currentTab} onChange={(value) => onTabChange(value as TilskuddBehandlingTab)}>
          <Tabs.List>{tabList}</Tabs.List>
          <Box marginBlock="space-16">
            <TwoColumnGrid separator>
              <Box>
                <Tabs.Panel value="saksopplysninger">{saksopplysningerContent}</Tabs.Panel>
                <Tabs.Panel value="vedtak">{vedtakContent}</Tabs.Panel>
              </Box>
              <DeltakerinformasjonOgBetalingsbetingelser deltaker={enkeltplassDeltaker} />
            </TwoColumnGrid>
          </Box>
        </Tabs>
        <Separator />
        {actions}
      </WhitePaddedBox>
    </>
  );
}
