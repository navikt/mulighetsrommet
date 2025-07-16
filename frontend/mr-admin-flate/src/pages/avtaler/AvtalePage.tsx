import { Header } from "@/components/detaljside/Header";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { Box, Heading, HStack, Tabs } from "@navikt/ds-react";
import { useMatch } from "react-router";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { AvtaleStatusMedAarsakTag } from "@/components/statuselementer/AvtaleStatusMedAarsakTag";
import { avtaleDetaljerTabAtom } from "@/api/atoms";
import { useAtom } from "jotai";
import { InlineErrorBoundary } from "@/ErrorBoundary";
import { RedaksjoneltInnholdPreview } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdPreview";
import { AvtaleDetaljer } from "./AvtaleDetaljer";
import { AvtalePersonvern } from "./AvtalePersonvern";
import { GjennomforingerForAvtalePage } from "../gjennomforing/GjennomforingerForAvtalePage";

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
  const { data: avtale } = useAvtale(avtaleId);

  const brodsmuler = useAvtaleBrodsmuler(avtale.id);

  const [activeTab, setActiveTab] = useAtom(avtaleDetaljerTabAtom);

  return (
    <>
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
            value="gjennomforinger"
            label="Gjennomføringer"
            onClick={() => setActiveTab("gjennomforinger")}
            aria-controls="panel"
          />
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
        </Tabs.List>
        <Box borderRadius="4" marginBlock="4" marginInline="2" padding="4" background="bg-default">
          <Tabs.Panel value="detaljer">
            <InlineErrorBoundary>
              <AvtaleDetaljer avtale={avtale} />
            </InlineErrorBoundary>
          </Tabs.Panel>
          <Tabs.Panel value="gjennomforinger">
            <InlineErrorBoundary>
              <GjennomforingerForAvtalePage />
            </InlineErrorBoundary>
          </Tabs.Panel>
          <Tabs.Panel value="personvern">
            <InlineErrorBoundary>
              <AvtalePersonvern avtale={avtale} />
            </InlineErrorBoundary>
          </Tabs.Panel>
          <Tabs.Panel value="redaksjonelt-innhold">
            <InlineErrorBoundary>
              <RedaksjoneltInnholdPreview
                tiltakstype={avtale.tiltakstype}
                beskrivelse={avtale.beskrivelse}
                faneinnhold={avtale.faneinnhold}
                kontorstruktur={avtale.kontorstruktur}
              />
            </InlineErrorBoundary>
          </Tabs.Panel>
        </Box>
      </Tabs>
    </>
  );
}
