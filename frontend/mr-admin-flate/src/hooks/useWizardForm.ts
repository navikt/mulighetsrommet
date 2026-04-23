import { zodResolver } from "@hookform/resolvers/zod";
import { JSX, useState } from "react";
import { DefaultValues, FieldValues, Resolver, SubmitHandler, useForm } from "react-hook-form";
import { ZodObject } from "zod";

export interface WizardStep {
  key: string;
  schema: ZodObject;
  Component: JSX.Element;
}

interface UseWizardOptions<TFormValues extends FieldValues> {
  steps: WizardStep[];
  defaultValues: DefaultValues<TFormValues>;
  onSubmit: (data: TFormValues) => void | Promise<void>;
  onCancel: () => void;
}

export function useWizardForm<TFormValues extends FieldValues>({
  steps,
  defaultValues,
  onSubmit,
  onCancel,
}: UseWizardOptions<TFormValues>) {
  const [activeStep, setActiveStep] = useState(1);
  const [collectedData, setCollectedData] = useState<DefaultValues<TFormValues>>(defaultValues);

  const currentStep = steps[activeStep - 1];
  const isLastStep = activeStep === steps.length;

  const methods = useForm<TFormValues>({
    resolver: zodResolver(currentStep.schema as ZodObject<any>) as Resolver<TFormValues>,
    defaultValues: collectedData,
  });

  const handleStepChange = async (val: number) => {
    await methods.trigger();
    if (methods.formState.isValid) {
      setActiveStep(val);
    }
  };

  const handleStepBack = () => {
    if (activeStep === 1) {
      onCancel();
    } else {
      setActiveStep(activeStep - 1);
    }
  };

  const handleStepForward: SubmitHandler<TFormValues> = async (data) => {
    const mergedData = { ...collectedData, ...data } as DefaultValues<TFormValues>;
    setCollectedData(mergedData);

    if (!isLastStep) {
      setActiveStep(activeStep + 1);
    } else {
      await onSubmit(mergedData as TFormValues);
    }
  };

  return {
    activeStep,
    currentStep,
    isLastStep,
    methods,
    handleStepChange,
    handleStepBack,
    handleStepForward,
  };
}
