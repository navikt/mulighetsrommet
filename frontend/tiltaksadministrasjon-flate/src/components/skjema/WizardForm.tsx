import { ValideringsfeilOppsummering } from "@/components/skjema/ValideringsfeilOppsummering";
import { useWizardForm, WizardStep } from "@/hooks/useWizardForm";
import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { Box, Button, HStack, Stepper, VStack } from "@navikt/ds-react";
import { DefaultValues, FieldValues, FormProvider, UseFormReturn } from "react-hook-form";

interface WizardFormProps<TFormValues extends FieldValues> {
  steps: WizardStep<TFormValues>[];
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
    isValidating,
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
      <Stepper
        aria-label="Stepper"
        activeStep={activeStep}
        onStepChange={(step) => {
          if (!isValidating) void handleStepChange(step);
        }}
        orientation="horizontal"
      >
        {steps.map((step) => (
          <Stepper.Step key={step.key} className="whitespace-nowrap">
            {step.key}
          </Stepper.Step>
        ))}
      </Stepper>
      <Separator />
      <FormProvider {...form}>
        <form onSubmit={form.handleSubmit(handleStepForward)}>
          <VStack gap="space-8">
            {currentStep.Component}
            <Separator />
            <HStack gap="space-8" justify="end">
              <ValideringsfeilOppsummering />
              <Button
                size="small"
                type="button"
                variant="tertiary"
                onClick={handleStepBack}
                disabled={isValidating}
              >
                {activeStep === 1 ? cancel : back}
              </Button>
              <Button
                size="small"
                type={isLastStep ? "submit" : "button"}
                loading={isSubmitting || isValidating}
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
