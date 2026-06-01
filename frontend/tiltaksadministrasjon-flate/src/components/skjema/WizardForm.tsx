import { ValideringsfeilOppsummering } from "@/components/skjema/ValideringsfeilOppsummering";
import { useWizardForm, WizardStep } from "@/hooks/useWizardForm";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { Box, Button, FormProgress, Heading, HStack, VStack } from "@navikt/ds-react";
import { DefaultValues, FieldValues, FormProvider, UseFormReturn } from "react-hook-form";

interface WizardFormProps<TFormValues extends FieldValues> {
  steps: WizardStep[];
  defaultValues: DefaultValues<TFormValues>;
  onCancel: () => void;
  onSubmit: (data: TFormValues, methods: UseFormReturn<TFormValues>) => void | Promise<void>;
  isSubmitting?: boolean;
  labels?: WizardLabels;
}

interface WizardLabels {
  submit?: string;
  cancel?: string;
  next?: string;
  back?: string;
}

export function WizardForm<TFormValues extends FieldValues>({
  steps,
  defaultValues,
  onCancel,
  onSubmit,
  isSubmitting,
  labels = {},
}: WizardFormProps<TFormValues>) {
  const {
    activeStep,
    currentStep,
    isLastStep,
    form,
    handleStepChange,
    handleStepBack,
    handleStepForward,
  } = useWizardForm<TFormValues>({ steps, defaultValues, onSubmit, onCancel });

  const { cancel = "Avbryt", next = "Neste", submit = "Send inn", back = "Tilbake" } = labels;

  return (
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
      <FormProgress
        totalSteps={steps.length}
        activeStep={activeStep}
        onStepChange={handleStepChange}
      >
        {steps.map((step) => (
          <FormProgress.Step key={step.key}>{step.key}</FormProgress.Step>
        ))}
      </FormProgress>
      <Separator />
      <FormProvider {...form}>
        <form onSubmit={form.handleSubmit(handleStepForward)}>
          <VStack gap="space-8">
            {currentStep.Component}
            <Separator />
            <HStack gap="space-8" justify="end">
              <ValideringsfeilOppsummering />
              <Button size="small" type="button" variant="tertiary" onClick={handleStepBack}>
                {activeStep === 1 ? cancel : back}
              </Button>
              <Button
                size="small"
                type={isLastStep ? "submit" : "button"}
                loading={isSubmitting}
                onClick={form.handleSubmit(handleStepForward)}
              >
                {isLastStep ? submit : next}
              </Button>
            </HStack>
          </VStack>
        </form>
      </FormProvider>
    </Box>
  );
}
