import { useGjennomforing } from "@/api/gjennomforing/useGjennomforing";
import { useOpprettTilskuddBehandling } from "@/api/tilskudd-behandling/mutations";
import { Header } from "@/components/detaljside/Header";
import { GjennomforingDetaljerMini } from "@/components/gjennomforing/GjennomforingDetaljerMini";
import { Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ValideringsfeilOppsummering } from "@/components/skjema/ValideringsfeilOppsummering";
import { SaksopplysningerForm } from "@/components/tilskudd-behandling/Saksopplysninger";
import { VedtakForm } from "@/components/tilskudd-behandling/VedtakForm";
import { useRequiredParams } from "@/hooks/useRequiredParams";
import { TwoColumnGrid } from "@/layouts/TwoColumGrid";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { TilskuddBehandlingRequest, ValidationError } from "@tiltaksadministrasjon/api-client";
import { GavelSoundBlockFillIcon } from "@navikt/aksel-icons";
import { Box, Button, Heading, HStack, Tabs } from "@navikt/ds-react";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useNavigate } from "react-router";
import { v4 } from "uuid";
import { defaultVedtakRequest } from "@/components/tilskudd-behandling/defaultVedtakRequest";

const tabs = [
  { key: "saksopplysninger", label: "Saksopplysninger" },
  { key: "vedtak", label: "Vedtak" },
];

export function TilskuddBehandlingFormPage() {
  const { gjennomforingId } = useRequiredParams(["gjennomforingId"]);
  const { gjennomforing } = useGjennomforing(gjennomforingId);
  const [currentTab, setCurrentTab] = useState(tabs[0].key);
  const navigate = useNavigate();
  const mutation = useOpprettTilskuddBehandling(gjennomforingId);

  const methods = useForm<TilskuddBehandlingRequest>({
    defaultValues: {
      id: v4(),
      gjennomforingId,
      periodeSlutt: null,
      periodeStart: null,
      soknadJournalpostId: null,
      kostnadssted: null,
      soknadDato: null,
      vedtak: [defaultVedtakRequest],
    },
    mode: "onBlur",
  });

  const { handleSubmit, setError } = methods;

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

  const listUrl = `/gjennomforinger/${gjennomforingId}/tilskudd-behandling`;

  const onSubmit = handleSubmit((data) => {
    mutation.mutate(data, {
      onSuccess: () => navigate(listUrl),
      onValidationError: (error: ValidationError) => {
        error.errors.forEach((fieldError) => {
          const name = jsonPointerToFieldPath(
            fieldError.pointer,
          ) as keyof TilskuddBehandlingRequest;
          setError(name, { type: "custom", message: fieldError.detail });
        });
      },
    });
  });

  return (
    <FormProvider {...methods}>
      <form onSubmit={onSubmit}>
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
              lenke: `/gjennomforinger/${gjennomforingId}/tilskudd-behandling` as const,
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
          <Tabs value={currentTab} onChange={setCurrentTab}>
            <Tabs.List>
              {tabs.map((tab) => (
                <Tabs.Tab key={tab.key} value={tab.key} label={tab.label} />
              ))}
            </Tabs.List>
            <Box marginBlock="space-16">
              <TwoColumnGrid separator>
                <Box>
                  <Tabs.Panel value="saksopplysninger">
                    <div>
                      <SaksopplysningerForm />
                    </div>
                  </Tabs.Panel>
                  <Tabs.Panel value="vedtak">
                    <VedtakForm />
                  </Tabs.Panel>
                </Box>
                <Heading size="medium" level="3" spacing>
                  Oppsummering
                </Heading>
              </TwoColumnGrid>
            </Box>
          </Tabs>
          <Separator />
          <HStack gap="space-8" marginBlock="space-16" justify="end">
            {isFirstTab ? (
              <Button
                variant="tertiary"
                size="small"
                type="button"
                onClick={() => navigate(listUrl)}
              >
                Avbryt
              </Button>
            ) : (
              <Button variant="tertiary" size="small" type="button" onClick={goToPreviousTab}>
                Tilbake
              </Button>
            )}
            {isLastTab ? (
              <HStack gap="space-4" align="center">
                <ValideringsfeilOppsummering />
                <Button variant="primary" size="small" type="submit" disabled={mutation.isPending}>
                  {mutation.isPending ? "Sender til attestering..." : "Send til attestering"}
                </Button>
              </HStack>
            ) : (
              <Button variant="primary" size="small" type="button" onClick={goToNextTab}>
                Neste
              </Button>
            )}
          </HStack>
        </WhitePaddedBox>
      </form>
    </FormProvider>
  );
}
