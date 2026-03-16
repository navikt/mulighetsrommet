import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { Header } from "@/components/detaljside/Header";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { GavelSoundBlockFillIcon } from "@navikt/aksel-icons";
import { Box, Button, Heading, HStack, Tabs } from "@navikt/ds-react";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { Saksopplysninger } from "./Saksopplysninger";
import { Vilkarsvurdering } from "./Vilkarsvurdering";
import { Vedtak } from "./Vedtak";
import { Link, useNavigate } from "react-router";
import type { BehandlingFormData } from "./schema";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { Behandlingsdetaljer } from "./Behandlingsdetaljer";

const tabs = [
  { key: "Saksopplysninger", label: "Saksopplysninger" },
  { key: "vilkarsvurdering", label: "Vilkårsvurdering" },
  { key: "Vedtak", label: "Vedtak" },
];

export function BehandlingPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { gjennomforing } = useGjennomforing(gjennomforingId);
  const [currentTab, setCurrentTab] = useState(tabs[0].key);
  const navigate = useNavigate();

  const methods = useForm<BehandlingFormData>({
    defaultValues: {
      journalpostId: "",
      soknadstidspunkt: undefined,
      tilskudd: [
        {
          tilskuddstype: "",
          belop: "",
          belopTilUtbetaling: "",
          nodvendigForOpplaring: undefined,
          begrunnelse: "",
          vedtaksresultat: undefined,
        },
      ],
      belopInnenforMaksgrense: undefined,
      unntakVurdert: undefined,
      maksbelopBegrunnelse: "",
      mottakerAvUtbetaling: undefined,
      kommentarerTilDeltaker: "",
    },
    mode: "onBlur",
  });

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
    <FormProvider {...methods}>
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
        <WhitePaddedBox>
          <GjennomforingDetaljerMini gjennomforing={gjennomforing} />
          <Separator />
          <Tabs value={currentTab} onChange={setCurrentTab}>
            <Tabs.List>
              {tabs.map((tab) => (
                <Tabs.Tab key={tab.key} value={tab.key} label={tab.label} />
              ))}
            </Tabs.List>
            <Box marginBlock="space-16">
              <TwoColumnGrid separator>
                <Box>
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
                <Heading size="medium" level="3" spacing>
                  Oppsummering
                </Heading>
                <Behandlingsdetaljer
                  journalpostId={methods.watch("journalpostId")}
                  soknadstidspunkt={methods.watch("soknadstidspunkt")}
                  tilskudd={methods.watch("tilskudd")}
                  belopInnenforMaksgrense={methods.watch("belopInnenforMaksgrense")}
                  maksbelopBegrunnelse={methods.watch("maksbelopBegrunnelse")}
                />
              </TwoColumnGrid>
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
              <Button
                variant="primary"
                size="small"
                onClick={() =>
                  navigate(`/gjennomforinger/${gjennomforingId}/tilskuddsbehandlinger`)
                }
              >
                Send til attestering
              </Button>
            ) : (
              <Button variant="primary" size="small" onClick={goToNextTab}>
                Neste
              </Button>
            )}
          </HStack>
        </WhitePaddedBox>
      </>
    </FormProvider>
  );
}
