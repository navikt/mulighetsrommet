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
  gjennomforingDetaljerWizardSchema,
  gjennomforingVeilederinfoWizardSchema,
} from "@/schemas/gjennomforing";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { zodResolver } from "@hookform/resolvers/zod";
import { Box, Button, Heading, HStack, Stepper, VStack } from "@navikt/ds-react";
import { JSX, useState } from "react";
import { DeepPartial, FieldValues, FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { useLocation, useNavigate } from "react-router";
import { ValidationError } from "@tiltaksadministrasjon/api-client";
import { GjennomforingFormValues, toCreateGjennomforingRequest } from "./gjennomforingFormUtils";
import { applyValidationErrors } from "@/components/skjema/helpers";
import { ZodObject } from "zod";
import { v4 as uuidv4 } from "uuid";

const brodsmuler: Array<Brodsmule | undefined> = [
  { tittel: "Gjennomføringer", lenke: "/gjennomforinger" },
  { tittel: "Ny gjennomføring" },
];

interface Step {
  key: string;
  schema: ZodObject;
  Component: JSX.Element;
}

export function OpprettGjennomforingPage() {
  const avtaleId = useGetAvtaleIdFromUrlOrThrow();
  const location = useLocation();
  const navigate = useNavigate();
  const { data: avtale } = useAvtale(avtaleId);
  const tiltakstype = useTiltakstype(avtale.tiltakstype.id);
  const { data: ansatt } = useHentAnsatt();
  const createGjennomforing = useCreateGjennomforing();

  const [activeStep, setActiveStep] = useState(1);
  const [collectedData, setCollectedData] = useState<DeepPartial<GjennomforingFormValues>>(
    defaultGjennomforingData(
      ansatt,
      tiltakstype,
      avtale,
      location.state?.dupliserGjennomforing?.gjennomforing,
      location.state?.dupliserGjennomforing?.veilederinfo,
      null,
      null,
      null,
    ),
  );

  const steps: Step[] = [
    {
      key: "Detaljer",
      schema: gjennomforingDetaljerWizardSchema,
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
      schema: gjennomforingVeilederinfoWizardSchema,
      Component: <GjennomforingInformasjonForVeiledereForm avtale={avtale} veilederinfo={null} />,
    },
  ];

  const currentStep = steps[activeStep - 1];
  const methods = useForm({
    resolver: zodResolver(currentStep.schema as ZodObject<any>),
    defaultValues: collectedData,
    mode: "onSubmit",
  });

  const handleBackStep = () => {
    if (activeStep === 1) {
      navigate(-1);
    } else {
      setActiveStep(activeStep - 1);
    }
  };

  const handleStepChange = (val: number) => {
    methods.trigger();
    if (methods.formState.isValid) {
      setActiveStep(val);
    }
  };

  const handleForwardStep: SubmitHandler<FieldValues> = (data) => {
    const mergedData = { ...collectedData, ...data };
    setCollectedData(mergedData);

    if (activeStep < steps.length) {
      setActiveStep(activeStep + 1);
    } else {
      const id = uuidv4();
      const request = toCreateGjennomforingRequest(id, mergedData, avtale);
      createGjennomforing.mutate(request, {
        onSuccess: () => navigate(`/gjennomforinger/${id}`),
        onValidationError: (error: ValidationError) => applyValidationErrors(methods, error),
      });
    }
  };

  return (
    <>
      <title>Opprett gjennomføring</title>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <GjennomforingIkon />
        <Heading size="large" level="2">
          Opprett ny gjennomføring
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
          <form onSubmit={methods.handleSubmit(handleForwardStep)}>
            <VStack gap="space-8">
              {currentStep.Component}
              <Separator />
              <HStack gap="space-8" justify="end">
                <ValideringsfeilOppsummering />
                <Button size="small" type="button" variant="tertiary" onClick={handleBackStep}>
                  {activeStep === 1 ? "Avbryt" : "Tilbake"}
                </Button>
                <Button
                  size="small"
                  type={isLastStep ? "submit" : "button"}
                  loading={createGjennomforing.isPending}
                  onClick={methods.handleSubmit(handleForwardStep)}
                >
                  {activeStep === steps.length ? "Opprett gjennomføring" : "Neste"}
                </Button>
              </HStack>
            </VStack>
          </form>
        </FormProvider>
      </Box>
    </>
  );
}
