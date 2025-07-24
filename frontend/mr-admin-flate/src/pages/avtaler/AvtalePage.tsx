import { Header } from "@/components/detaljside/Header";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { Box, Heading, HStack, Tabs } from "@navikt/ds-react";
import { useLocation, useMatch, useNavigate } from "react-router";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { AvtaleStatusMedAarsakTag } from "@/components/statuselementer/AvtaleStatusMedAarsakTag";
import { avtaleDetaljerTabAtom } from "@/api/atoms";
import { useAtom } from "jotai";
import { RedaksjoneltInnholdPreview } from "@/components/redaksjoneltInnhold/RedaksjoneltInnholdPreview";
import { AvtaleDetaljer } from "./AvtaleDetaljer";
import { AvtalePersonvern } from "./AvtalePersonvern";
import { GjennomforingerForAvtalePage } from "../gjennomforing/GjennomforingerForAvtalePage";
import { QueryKeys } from "@/api/QueryKeys";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useTiltakstyper } from "@/api/tiltakstyper/useTiltakstyper";
import { useQueryClient } from "@tanstack/react-query";
import { RedigerAvtaleContainer } from "@/components/avtaler/RedigerAvtaleContainer";
import { AvtaleFormDetaljer } from "@/components/avtaler/AvtaleFormDetaljer";
import { defaultAvtaleData } from "@/schemas/avtale";
import { AvtalePersonvernForm } from "@/components/avtaler/AvtalePersonvernForm";
import { AvtalePageLayout } from "./AvtalePageLayout";
import { InlineErrorBoundary } from "@/ErrorBoundary";

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
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const location = useLocation();

  const { data: avtale } = useAvtale(avtaleId);
  const { data: tiltakstyper } = useTiltakstyper();
  const { data: ansatt } = useHentAnsatt();

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
          />
        </Tabs.List>
        <Box borderRadius="4" marginBlock="4" marginInline="2" padding="4" background="bg-default">
          <Tabs.Panel value="detaljer">
            {location.pathname.includes("skjema") ? (
              <RedigerAvtaleContainer
                onSuccess={async (id) => {
                  await queryClient.invalidateQueries({
                    queryKey: QueryKeys.avtale(avtale?.id),
                    refetchType: "all",
                  });
                  navigate(`/avtaler/${id}`);
                }}
                avtale={avtale}
                defaultValues={defaultAvtaleData(ansatt, location.state?.dupliserAvtale ?? avtale)}
              >
                <AvtaleFormDetaljer tiltakstyper={tiltakstyper} ansatt={ansatt} avtale={avtale} />
              </RedigerAvtaleContainer>
            ) : (
              <AvtalePageLayout avtale={avtale} ansatt={ansatt}>
                <AvtaleDetaljer avtale={avtale} />
              </AvtalePageLayout>
            )}
          </Tabs.Panel>
          <Tabs.Panel value="gjennomforinger">
            <InlineErrorBoundary>
              <GjennomforingerForAvtalePage />
            </InlineErrorBoundary>
          </Tabs.Panel>
          <Tabs.Panel value="personvern">
            {location.pathname.includes("skjema") ? (
              <RedigerAvtaleContainer
                onSuccess={async (id) => {
                  await queryClient.invalidateQueries({
                    queryKey: QueryKeys.avtale(avtale?.id),
                    refetchType: "all",
                  });
                  navigate(`/avtaler/${id}`);
                }}
                avtale={avtale}
                defaultValues={defaultAvtaleData(ansatt, location.state?.dupliserAvtale ?? avtale)}
              >
                <AvtalePersonvernForm />
              </RedigerAvtaleContainer>
            ) : (
              <AvtalePageLayout avtale={avtale} ansatt={ansatt}>
                <AvtalePersonvern avtale={avtale} />
              </AvtalePageLayout>
            )}
          </Tabs.Panel>
          <Tabs.Panel value="redaksjonelt-innhold">
            <AvtalePageLayout avtale={avtale} ansatt={ansatt}>
              <RedaksjoneltInnholdPreview
                tiltakstype={avtale.tiltakstype}
                beskrivelse={avtale.beskrivelse}
                faneinnhold={avtale.faneinnhold}
                kontorstruktur={avtale.kontorstruktur}
              />
            </AvtalePageLayout>
          </Tabs.Panel>
        </Box>
      </Tabs>
    </>
  );
}
