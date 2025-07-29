import { useHentAnsatt } from "@/api/ansatt/useHentAnsatt";
import { useUpsertAvtale } from "@/api/avtaler/useUpsertAvtale";
import { HarSkrivetilgang } from "@/components/authActions/HarSkrivetilgang";
import { AvtaleDetaljerForm } from "@/components/avtaler/AvtaleDetaljerForm";
import { AvtalePersonvernForm } from "@/components/avtaler/AvtalePersonvernForm";
import { AvtaleRedaksjoneltInnholdForm } from "@/components/avtaler/AvtaleRedaksjoneltInnholdForm";
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
  RedaksjoneltInnholdSchema,
} from "@/schemas/avtale";
import { avtaleDetaljerFormSchema, getUtdanningslop } from "@/schemas/avtaledetaljer";
import { zodResolver } from "@hookform/resolvers/zod";
import { Prismodell, AvtaleDto, ValidationError } from "@mr/api-client-v2";
import { jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";
import { Box, Button, Heading, HStack, Stepper, VStack } from "@navikt/ds-react";
import { useCallback, useState } from "react";
import { DeepPartial, FieldValues, FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { useNavigate } from "react-router";
import { v4 as uuidv4 } from "uuid";
import { ZodObject } from "zod";

const steps = [
  {
    key: "Detaljer",
    schema: avtaleDetaljerFormSchema,
    Component: <AvtaleDetaljerForm />,
  },
  {
    key: "Personvern",
    schema: PersonopplysningerSchema,
    Component: <AvtalePersonvernForm />,
  },
  {
    key: "Redaksjonelt",
    schema: RedaksjoneltInnholdSchema,
    Component: <AvtaleRedaksjoneltInnholdForm />,
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
  const mutation = useUpsertAvtale();
  const { data: ansatt } = useHentAnsatt();
  const [activeStep, setActiveStep] = useState<number>(1);
  const [collectedData, setCollectedData] = useState<DeepPartial<AvtaleFormValues>>(
    defaultAvtaleData(ansatt),
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

      function mapNameToSchemaPropertyName(name: string) {
        const mapping: { [name: string]: string } = {
          opsjonsmodell: "opsjonsmodell.type",
          opsjonMaksVarighet: "opsjonsmodell.opsjonMaksVarighet",
          customOpsjonsmodellNavn: "opsjonsmodell.customOpsjonsmodellNavn",
          tiltakstypeId: "tiltakstype",
          utdanningslop: "utdanningslop.utdanninger",
        };
        return (mapping[name] ?? name) as keyof AvtaleFormValues;
      }
    },
    [methods],
  );

  const onSubmit = async (data: AvtaleFormValues) => {
    const {
      navn,
      startDato,
      beskrivelse,
      avtaletype,
      faneinnhold,
      personopplysninger,
      personvernBekreftet,
      satser,
      administratorer,
    } = data;
    const requestBody = {
      id: uuidv4(),
      navn,
      administratorer,
      beskrivelse,
      faneinnhold,
      personopplysninger,
      personvernBekreftet,
      satser,
      avtaletype,
      startDato,
      sakarkivNummer: data.sakarkivNummer || null,
      sluttDato: data.sluttDato || null,
      navEnheter: data.navRegioner.concat(data.navKontorer).concat(data.navAndreEnheter),
      avtalenummer: null,
      arrangor:
        data.arrangorHovedenhet && data.arrangorUnderenheter
          ? {
              hovedenhet: data.arrangorHovedenhet,
              underenheter: data.arrangorUnderenheter,
              kontaktpersoner: data.arrangorKontaktpersoner || [],
            }
          : null,
      tiltakstypeId: data.tiltakstype.id,
      prisbetingelser:
        !data.prismodell || data.prismodell === Prismodell.ANNEN_AVTALT_PRIS
          ? data.prisbetingelser || null
          : null,
      amoKategorisering: data.amoKategorisering || null,
      opsjonsmodell: {
        type: data.opsjonsmodell.type,
        opsjonMaksVarighet: data.opsjonsmodell.opsjonMaksVarighet || null,
        customOpsjonsmodellNavn: data.opsjonsmodell.customOpsjonsmodellNavn || null,
      },
      utdanningslop: getUtdanningslop(data),
      prismodell: data.prismodell ?? Prismodell.ANNEN_AVTALT_PRIS,
    };

    mutation.mutate(requestBody, {
      onSuccess: (dto: { data: AvtaleDto }) => {
        navigate(`/avtaler/${dto.data.id}`);
      },
      onValidationError: (error: ValidationError) => {
        handleValidationError(error);
      },
    });
  };

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

    if (activeStep !== 3) {
      setActiveStep(activeStep + 1);
    } else {
      const result = avtaleFormSchema.safeParse(mergedData);
      if (result.success) {
        onSubmit(result.data);
      } else
        result.error.errors.forEach((err) => {
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
      <HarSkrivetilgang ressurs="Avtale">
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
                    {activeStep === 3 ? "Opprett avtale" : "Neste"}
                  </Button>
                </HStack>
              </VStack>
            </form>
          </FormProvider>
        </Box>
      </HarSkrivetilgang>
    </>
  );
}
