import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { Header } from "@/components/detaljside/Header";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { GavelSoundBlockFillIcon } from "@navikt/aksel-icons";
import { Box, Button, Heading, HStack, Tabs } from "@navikt/ds-react";
import { useState } from "react";
import { Saksopplysninger } from "./Saksopplysninger";
import { Vilkarsvurdering } from "./Vilkarsvurdering";
import { Vedtak } from "./Vedtak";
import { Link } from "react-router";

const tabs = [
  { key: "Saksopplysninger", label: "Saksopplysninger" },
  { key: "vilkarsvurdering", label: "Vilkårsvurdering" },
  { key: "Vedtak", label: "Vedtak" },
];

export function BehandlingPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { gjennomforing } = useGjennomforing(gjennomforingId);
  const [currentTab, setCurrentTab] = useState(tabs[0].key);

  const currentTabIndex = tabs.findIndex((tab) => tab.key === currentTab);
  const isFirstTab = currentTabIndex === 0;
  const isLastTab = currentTabIndex === tabs.length - 1;

  const goToPreviousTab = () => {
    if (!isFirstTab) {
      setCurrentTab(tabs[currentTabIndex - 1].key);
    }
  };

  const goToNextTab = () => {
    if (!isLastTab) {
      setCurrentTab(tabs[currentTabIndex + 1].key);
    }
  };
  return (
    <>
      <title>Tilskuddsbehandling</title>
      <Brodsmuler
        brodsmuler={[
          {
            tittel: "Gjennomføringer",
            lenke: `/gjennomforinger`,
          },
          {
            tittel: "Gjennomføring",
            lenke: `/gjennomforinger/${gjennomforingId}`,
          },
          {
            tittel: "Tilskuddsbehandlinger",
            lenke: `/gjennomforinger/${gjennomforingId}/tilskuddsbehandlinger`,
          },
          { tittel: "Opprett behandling" },
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
      <ContentBox>
        <WhitePaddedBox>
          <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
          <Separator />
          <Tabs value={currentTab} onChange={setCurrentTab}>
            <Tabs.List>
              {tabs.map((tab) => (
                <Tabs.Tab key={tab.key} value={tab.key} label={tab.label} />
              ))}
            </Tabs.List>
            <Box marginBlock="space-20">
              <Tabs.Panel value="Saksopplysninger">
                <Saksopplysninger />
              </Tabs.Panel>
              <Tabs.Panel value="vilkarsvurdering">
                <Vilkarsvurdering />
              </Tabs.Panel>
              <Tabs.Panel value="Vedtak">
                <Vedtak />
              </Tabs.Panel>
            </Box>
          </Tabs>
          <Separator />
          <HStack gap="space-8" marginBlock="space-16" justify="end">
            {isFirstTab ? (
              <Button
                as={Link}
                to={`/gjennomforinger/${gjennomforingId}/tilskuddsbehandlinger`}
                variant="tertiary"
                size="small"
              >
                Avbryt
              </Button>
            ) : (
              <Button variant="tertiary" size="small" onClick={goToPreviousTab}>
                Tilbake
              </Button>
            )}
            {isLastTab ? (
              <Button variant="primary" size="small">
                Send til attestering
              </Button>
            ) : (
              <Button variant="primary" size="small" onClick={goToNextTab}>
                Neste
              </Button>
            )}
          </HStack>
        </WhitePaddedBox>
      </ContentBox>
    </>
  );
}
