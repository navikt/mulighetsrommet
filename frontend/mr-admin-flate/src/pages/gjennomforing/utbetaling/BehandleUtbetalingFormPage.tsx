import { useOpprettUtbetaling } from "@/api/utbetaling/useOpprettUtbetaling";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  ProblemDetail,
  RefusjonKravKompakt,
  TilsagnDto,
  TilsagnStatus,
  UtbetalingRequest,
  FieldError,
} from "@mr/api-client-v2";
import { Button, Heading, HStack, Stepper, VStack } from "@navikt/ds-react";
import { DeepPartial, FormProvider, SubmitHandler, useForm } from "react-hook-form";
import { useLoaderData, useNavigate } from "react-router";
import { behandleUtbetalingFormPageLoader } from "./behandleUtbetalingFormPageLoader";
import { ContentBox } from "@/layouts/ContentBox";
import { WhitePaddedBox } from "@/layouts/WhitePaddedBox";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { Header } from "@/components/detaljside/Header";
import { GjennomforingIkon } from "@/components/ikoner/GjennomforingIkon";
import {
  InferredUtbetalingSchema,
  UtbetalingSchema,
} from "@/components/utbetaling/UtbetalingSchema";
import { KostnadsfordelingSteg } from "@/components/utbetaling/KostnadsfordelingSteg";
import { useState } from "react";
import { isValidationError, jsonPointerToFieldPath } from "@mr/frontend-common/utils/utils";

// TODO: Potensielt flyttes til backend
function defaultValues(
  krav: RefusjonKravKompakt,
  tilsagn: TilsagnDto[],
): DeepPartial<InferredUtbetalingSchema> {
  const kunEttTilsagn = tilsagn.length === 1;
  return {
    kostnadsfordeling: tilsagn.map((t) => ({
      tilsagnId: t.id,
      belop:
        kunEttTilsagn && t.status.type === TilsagnStatus.GODKJENT
          ? Math.min(krav.beregning.belop, t.beregning.output.belop)
          : 0,
    })),
  };
}

export function BehandleUtbetalingFormPage() {
  const { gjennomforing, krav, tilsagn } = useLoaderData<typeof behandleUtbetalingFormPageLoader>();

  const mutation = useOpprettUtbetaling(krav.id);
  const navigate = useNavigate();
  const [activeStep, setActiveStep] = useState<number>(2);

  const form = useForm<InferredUtbetalingSchema>({
    resolver: zodResolver(UtbetalingSchema),
    defaultValues: defaultValues(krav, tilsagn),
  });

  const { handleSubmit } = form;

  const postData: SubmitHandler<InferredUtbetalingSchema> = async (data): Promise<void> => {
    const body: UtbetalingRequest = {
      kostnadsfordeling: data.kostnadsfordeling,
    };

    mutation.mutate(body, {
      onSuccess: () => {
        navigate(-1);
      },
      onError: (error: ProblemDetail) => {
        if (isValidationError(error)) {
          error.errors.forEach((fieldError: FieldError) => {
            form.setError(
              jsonPointerToFieldPath(fieldError.pointer) as keyof InferredUtbetalingSchema,
              { type: "custom", message: fieldError.detail },
            );
          });
        }
      },
    });
  };

  const brodsmuler: Brodsmule[] = [
    { tittel: "Gjennomføringer", lenke: `/gjennomforinger` },
    {
      tittel: "Gjennomføring",
      lenke: `/gjennomforinger/${gjennomforing.id}`,
    },
    {
      tittel: "Utbetalinger",
      lenke: `/gjennomforinger/${gjennomforing.id}/utbetalinger`,
    },
    { tittel: "Behandle utbetaling" },
  ];

  return (
    <>
      <Brodsmuler brodsmuler={brodsmuler} />
      <Header>
        <GjennomforingIkon />
        <Heading size="large" level="2">
          <HStack gap="2" align={"center"}>
            Utbetaling for {gjennomforing.navn}
          </HStack>
        </Heading>
      </Header>
      <ContentBox>
        <WhitePaddedBox>
          <FormProvider {...form}>
            <form onSubmit={handleSubmit(postData)}>
              <VStack gap="4">
                <Stepper
                  activeStep={activeStep}
                  onStepChange={setActiveStep}
                  orientation="horizontal"
                >
                  <Stepper.Step completed>Utbetalingsinfo</Stepper.Step>
                  <Stepper.Step>Kostnadsfordeling</Stepper.Step>
                  <Stepper.Step>Oppsummering</Stepper.Step>
                </Stepper>
                {activeStep === 1 && <div>Utbetalingsinfo - not implemented</div>}
                {activeStep === 2 && (
                  <KostnadsfordelingSteg
                    gjennomforing={gjennomforing}
                    krav={krav}
                    tilsagn={tilsagn}
                  />
                )}
                {activeStep === 3 && <div>oppsummering - not implemented</div>}
                <HStack justify="space-between">
                  {activeStep > 1 ? (
                    <Button
                      size="small"
                      variant="secondary"
                      type="button"
                      onClick={() => setActiveStep(Math.max(activeStep - 1, 1))}
                    >
                      Tilbake
                    </Button>
                  ) : (
                    <div></div>
                  )}
                  {activeStep < 3 && (
                    <Button
                      size="small"
                      type="button"
                      variant="secondary"
                      onClick={() => setActiveStep(Math.min(activeStep + 1, 3))}
                    >
                      Neste
                    </Button>
                  )}
                  {activeStep === 3 && (
                    <Button size="small" variant="primary" type="submit">
                      Send til godkjenning
                    </Button>
                  )}
                </HStack>
              </VStack>
            </form>
          </FormProvider>
        </WhitePaddedBox>
      </ContentBox>
    </>
  );
}
