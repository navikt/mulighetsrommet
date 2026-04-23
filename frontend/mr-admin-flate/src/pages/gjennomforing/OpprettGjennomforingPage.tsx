import { useCreateGjennomforing } from "@/api/gjennomforing/useCreateGjennomforing";
import { Header } from "@/components/detaljside/Header";
import { defaultGjennomforingData } from "@/components/gjennomforing/GjennomforingFormConst";
import { GjennomforingFormDetaljer } from "@/components/gjennomforing/GjennomforingFormDetaljer";
import { GjennomforingInformasjonForVeiledereForm } from "@/components/gjennomforing/GjennomforingInformasjonForVeiledereForm";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ValideringsfeilOppsummering } from "@/components/skjema/ValideringsfeilOppsummering";
import { useGetAvtaleIdFromUrlOrThrow } from "@/hooks/useGetAvtaleIdFromUrl";
import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useAvtale } from "@/api/avtaler/useAvtale";
import { useTiltakstype } from "@/api/tiltakstyper/useTiltakstype";
import {
  gjennomforingVeilederinfoSchema,
  gjennomforingWizardSchema,
} from "@/schemas/gjennomforing";
import { useWizardForm, WizardStep } from "@/hooks/useWizardForm";
import { applyValidationErrors } from "@/components/skjema/helpers";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { Box, Button, Heading, HStack, Stepper, VStack } from "@navikt/ds-react";
import { FormProvider } from "react-hook-form";
import { useLocation, useNavigate } from "react-router";
import { ValidationError } from "@tiltaksadministrasjon/api-client";
import { GjennomforingFormValues, toCreateGjennomforingRequest } from "./gjennomforingFormUtils";
import { v4 as uuidv4 } from "uuid";

const brodsmuler: Array<Brodsmule | undefined> = [
  { tittel: "Gjennomføringer", lenke: "/gjennomforinger" },
  { tittel: "Opprett gjennomføring" },
];

export function OpprettGjennomforingPage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const location = useLocation();
  const navigate = useNavigate();
  const { data: avtale } = useAvtale(avtaleId);
  const tiltakstype = useTiltakstype(avtale.tiltakstype.id);
  const { data: ansatt } = useHentAnsatt();
  const createGjennomforing = useCreateGjennomforing();

  const steps: WizardStep[] = [
    {
      key: "Detaljer",
      schema: gjennomforingWizardSchema,
      Component: (
        <GjennomforingFormDetaljer
          tiltakstype={tiltakstype}
          avtale={avtale}
          gjennomforing={null}
          veilederinfo={null}
          deltakere={null}
        />
      ),
    },
    {
      key: "Informasjon for veiledere",
      schema: gjennomforingVeilederinfoSchema,
      Component: <GjennomforingInformasjonForVeiledereForm avtale={avtale} veilederinfo={null} />,
    },
  ];

  const {
    activeStep,
    currentStep,
    isLastStep,
    methods,
    handleStepChange,
    handleStepBack,
    handleStepForward,
  } = useWizardForm<GjennomforingFormValues>({
    steps,
    defaultValues: defaultGjennomforingData(
      ansatt,
      tiltakstype,
      avtale,
      location.state?.dupliserGjennomforing?.gjennomforing,
      location.state?.dupliserGjennomforing?.veilederinfo,
      null,
      null,
      null,
    ),
    onCancel: () => navigate(-1),
    onSubmit: (data) => {
      const id = uuidv4();
      const request = toCreateGjennomforingRequest(id, data, avtale);
      createGjennomforing.mutate(request, {
        onSuccess: () => navigate(`/gjennomforinger/${id}`),
        onValidationError: (error: ValidationError) => applyValidationErrors(methods, error),
      });
    },
  });

  return (
    <>
      <title>Opprett gjennomføring</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <GjennomforingIkon />
        <Heading size="large" level="2">
          Opprett gjennomføring
        </Heading>
      </Header>
      <Box
        borderRadius="4"
        marginBlock="space-16"
        marginInline="space-8"
        padding="space-16"
        background="default"
      >
        <Heading size="medium" spacing level="2" id="stepper-heading">
          Steg
        </Heading>
        <Stepper
          aria-labelledby="stepper-heading"
          activeStep={activeStep}
          onStepChange={handleStepChange}
          orientation="horizontal"
        >
          {steps.map((step) => (
            <Stepper.Step key={step.key}>{step.key}</Stepper.Step>
          ))}
        </Stepper>
        <Separator />
        <FormProvider {...methods}>
          <form onSubmit={methods.handleSubmit(handleStepForward)}>
            <VStack gap="space-8">
              {currentStep.Component}
              <Separator />
              <HStack gap="space-8" justify="end">
                <ValideringsfeilOppsummering />
                <Button size="small" type="button" variant="tertiary" onClick={handleStepBack}>
                  {activeStep === 1 ? "Avbryt" : "Tilbake"}
                </Button>
                <Button
                  size="small"
                  type="button"
                  loading={createGjennomforing.isPending}
                  onClick={methods.handleSubmit(handleStepForward)}
                >
                  {isLastStep ? "Opprett gjennomføring" : "Neste"}
                </Button>
              </HStack>
            </VStack>
          </form>
        </FormProvider>
      </Box>
    </>
  );
}
