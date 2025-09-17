import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useUpsertAvtale } from "@/api/avtaler/useUpsertAvtale";
import { QueryKeys } from "@/api/QueryKeys";
import { HarTilgang } from "@/components/auth/HarTilgang";
import { AvtaleDetaljerForm } from "@/components/avtaler/AvtaleDetaljerForm";
import { AvtalePersonvernForm } from "@/components/avtaler/AvtalePersonvernForm";
import { Header } from "@/components/detaljside/Header";
import { Separator } from "@/components/detaljside/Metadata";
import { AvtaleIkon } from "@/components/ikoner/AvtaleIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ValideringsfeilOppsummering } from "@/components/skjema/ValideringsfeilOppsummering";
import {
  avtaleFormSchema,
  AvtaleFormValues,
  defaultAvtaleData,
  PersonopplysningerSchema,
  PrismodellSchema,
  RedaksjoneltInnholdSchema,
} from "@/schemas/avtale";
import { avtaleDetaljerFormSchema } from "@/schemas/avtaledetaljer";
import { zodResolver } from "@hookform/resolvers/zod";
import { AvtaleDto, ValidationError } from "@mr/api-client-v2";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { Box, Button, Heading, HStack, Stepper, VStack } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useState } from "react";
import { DeepPartial, FieldValues, FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { useLocation, useNavigate } from "react-router";
import { ZodObject } from "zod";
import { mapNameToSchemaPropertyName, onSubmitAvtaleForm } from "./avtaleFormUtils";
import { AvtaleInformasjonForVeiledereForm } from "@/components/avtaler/AvtaleInformasjonForVeiledereForm";
import AvtalePrismodellStep from "@/components/avtaler/AvtalePrismodellStep";
import { Rolle } from "@tiltaksadministrasjon/api-client";

const steps = [
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
    key: "Redaksjonelt",
    schema: RedaksjoneltInnholdSchema,
    Component: <AvtaleInformasjonForVeiledereForm />,
  },
];

export type StepKey = (typeof steps)[number]["key"];

export function NewAvtaleFormPage() {
  const brodsmuler: Array<Brodsmule | undefined> = [
    { tittel: "Avtaler", lenke: "/avtaler" },
    {
      tittel: "Ny avtale",
    },
  ];

  const navigate = useNavigate();

  const queryClient = useQueryClient();
  const location = useLocation();
  const mutation = useUpsertAvtale();
  const { data: ansatt } = useHentAnsatt();
  const [activeStep, setActiveStep] = useState<number>(1);
  const [collectedData, setCollectedData] = useState<DeepPartial<AvtaleFormValues>>(
    defaultAvtaleData(ansatt, location.state?.dupliserAvtale),
  );

  const currentStep = steps[activeStep - 1];
  const methods = useForm({
    resolver: zodResolver(currentStep.schema as ZodObject<any>),
    defaultValues: collectedData,
    mode: "onSubmit",
  });

  const handleValidationError = useCallback(
    (validation: ValidationError) => {
      validation.errors.forEach((error) => {
        const name = mapNameToSchemaPropertyName(jsonPointerToFieldPath(error.pointer));
        methods.setError(name, { type: "custom", message: error.detail });
      });
    },
    [methods],
  );

  const onSubmit = async (data: AvtaleFormValues) =>
    onSubmitAvtaleForm({
      avtale: undefined,
      data,
      mutation,
      onValidationError: (error: ValidationError) => {
        handleValidationError(error);
      },
      onSuccess: (dto: { data: AvtaleDto }) => {
        queryClient.setQueryData(QueryKeys.avtale(dto.data.id), dto.data);
        navigate(`/avtaler/${dto.data.id}`);
      },
    });

  const handleStepChange = (val: number) => {
    methods.trigger();
    if (methods.formState.isValid) {
      setActiveStep(val);
    } else return;
  };
  const handleBackStep = () => {
    if (activeStep === 1) {
      navigate(-1);
    }
    setActiveStep(activeStep - 1);
  };

  const handleForwardStep: SubmitHandler<FieldValues> = async (data) => {
    const mergedData = { ...collectedData, ...data };
    setCollectedData(mergedData);

    if (activeStep !== 4) {
      setActiveStep(activeStep + 1);
    } else {
      const result = avtaleFormSchema.safeParse(mergedData);
      if (result.success) {
        onSubmit(result.data);
      } else
        result.error.issues.forEach((err) => {
          methods.setError(err.path.join("."), {
            type: "manual",
            message: err.message,
          });
        });
    }
  };

  return (
    <>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <AvtaleIkon />
        <Heading size="large" level="2">
          Opprett ny avtale
        </Heading>
      </Header>
      <HarTilgang rolle={Rolle.AVTALER_SKRIV}>
        <Box borderRadius="4" marginBlock="4" marginInline="2" padding="4" background="bg-default">
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
            <form onSubmit={handleForwardStep}>
              <VStack gap="2">
                {currentStep.Component}
                <Separator />
                <HStack gap="2" justify="end">
                  <ValideringsfeilOppsummering />
                  <Button
                    size="small"
                    type="button"
                    variant="tertiary"
                    onClick={() => handleBackStep()}
                  >
                    {activeStep === 1 ? "Avbryt" : "Tilbake"}
                  </Button>
                  <Button
                    size="small"
                    type="button"
                    onClick={methods.handleSubmit(handleForwardStep)}
                  >
                    {activeStep === 4 ? "Opprett avtale" : "Neste"}
                  </Button>
                </HStack>
              </VStack>
            </form>
          </FormProvider>
        </Box>
      </HarTilgang>
    </>
  );
}
