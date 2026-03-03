import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { Header } from "@/components/detaljside/Header";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { GavelSoundBlockFillIcon } from "@navikt/aksel-icons";
import { Button, Heading, HStack, Tabs } from "@navikt/ds-react";
import { Saksopplysninger } from "./Saksopplysninger";

const tabs = [
  { key: "Saksopplysninger", label: "Saksopplysninger" },
  { key: "vilkarsvurdering", label: "Vilkårsvurdering" },
  { key: "Vedtak", label: "Vedtak" },
];

export function BehandlingPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { gjennomforing } = useGjennomforing(gjennomforingId);
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
          <Tabs value="Vilkårsvurdering">
            <Tabs.List>
              {tabs.map((tab) => (
                <Tabs.Tab key={tab.key} value={tab.key} label={tab.label} />
              ))}
            </Tabs.List>
            <Tabs.Panel value="Saksopplysninger">
              <Saksopplysninger />
            </Tabs.Panel>
          </Tabs>
          <Separator />
          <HStack gap="space-8" marginBlock="space-16" justify="end">
            <Button variant="tertiary" size="small">
              Avbryt
            </Button>
            <Button variant="primary" size="small">
              Neste
            </Button>
          </HStack>
        </WhitePaddedBox>
      </ContentBox>
    </>
  );
}
