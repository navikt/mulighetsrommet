import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useOpprettAvtale } from "@/api/avtaler/useOpprettAvtale";
import { AvtaleDetaljerForm } from "@/components/avtaler/AvtaleDetaljerForm";
import { AvtalePersonvernForm } from "@/components/avtaler/AvtalePersonvernForm";
import { Header } from "@/components/detaljside/Header";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { applyValidationErrors } from "@/components/skjema/helpers";
import { ValideringsfeilOppsummering } from "@/components/skjema/ValideringsfeilOppsummering";
import {
  AvtaleFormValues,
  defaultAvtaleData,
  PersonopplysningerSchema,
  PrismodellSchema,
  VeilederinformasjonStepSchema,
} from "@/schemas/avtale";
import { avtaleDetaljerFormSchema } from "@/schemas/avtaledetaljer";
import { useWizardForm, WizardStep } from "@/hooks/useWizardForm";
import { ValidationError } from "@tiltaksadministrasjon/api-client";
import { Box, Button, Heading, HStack, Stepper, VStack } from "@navikt/ds-react";
import { useLocation, useNavigate } from "react-router";
import { FormProvider } from "react-hook-form";
import { toOpprettAvtaleRequest } from "./avtaleFormUtils";
import { AvtaleInformasjonForVeiledereForm } from "@/components/avtaler/AvtaleInformasjonForVeiledereForm";
import AvtalePrismodellStep from "@/components/avtaler/AvtalePrismodellStep";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { v4 as uuidv4 } from "uuid";

const steps: WizardStep[] = [
  {
    key: "Detaljer",
    schema: avtaleDetaljerFormSchema,
    Component: <AvtaleDetaljerForm />,
  },
  {
    key: "Prismodell",
    schema: PrismodellSchema,
    Component: <AvtalePrismodellStep />,
  },
  {
    key: "Personvern",
    schema: PersonopplysningerSchema,
    Component: <AvtalePersonvernForm />,
  },
  {
    key: "Veilederinformasjon",
    schema: VeilederinformasjonStepSchema,
    Component: <AvtaleInformasjonForVeiledereForm />,
  },
];

export function OpprettAvtaleFormPage() {
  const brodsmuler: Array<Brodsmule | undefined> = [
    { tittel: "Avtaler", lenke: "/avtaler" },
    { tittel: "Ny avtale" },
  ];

  const navigate = useNavigate();
  const location = useLocation();
  const opprettAvtale = useOpprettAvtale();
  const { data: ansatt } = useHentAnsatt();

  const {
    activeStep,
    currentStep,
    isLastStep,
    methods,
    handleStepChange,
    handleStepBack,
    handleStepForward,
  } = useWizardForm<AvtaleFormValues>({
    steps,
    defaultValues: defaultAvtaleData(ansatt, location.state?.dupliserAvtale),
    onCancel: () => navigate("/avtaler"),
    onSubmit: (data) => {
      const id = uuidv4();
      opprettAvtale.mutate(toOpprettAvtaleRequest(id, data), {
        onSuccess: () => navigate(`/avtaler/${id}`),
        onValidationError: (error: ValidationError) => applyValidationErrors(methods, error),
      });
    },
  });

  return (
    <>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <AvtaleIkon />
        <Heading size="large" level="2">
          Opprett ny avtale
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
                  onClick={methods.handleSubmit(handleStepForward)}
                >
                  {isLastStep ? "Opprett avtale" : "Neste"}
                </Button>
              </HStack>
            </VStack>
          </form>
        </FormProvider>
      </Box>
    </>
  );
}
