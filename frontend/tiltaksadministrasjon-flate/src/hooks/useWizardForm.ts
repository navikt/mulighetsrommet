import { zodResolver } from "@hookform/resolvers/zod";
import { JSX, useRef, useState } from "react";
import { ValidationError } from "@tiltaksadministrasjon/api-client";
import { applyValidationErrors } from "../components/skjema/helpers";
import {
  DefaultValues,
  FieldValues,
  Resolver,
  SubmitHandler,
  useForm,
  UseFormReturn,
} from "react-hook-form";
import { ZodObject } from "zod";

export interface WizardStep<TFormValues extends FieldValues> {
  key: string;
  schema: ZodObject;
  Component: JSX.Element;
  validateAsync?: (data: TFormValues) => Promise<ValidationError | undefined>;
}

interface UseWizardOptions<TFormValues extends FieldValues> {
  steps: WizardStep<TFormValues>[];
  defaultValues: DefaultValues<TFormValues>;
  onSubmit: (data: TFormValues, form: UseFormReturn<TFormValues>) => void | Promise<void>;
  onCancel: () => void;
}

export function useWizardForm<TFormValues extends FieldValues>({
  steps,
  defaultValues,
  onSubmit,
  onCancel,
}: UseWizardOptions<TFormValues>) {
  const [activeStep, setActiveStep] = useState(1);
  const [isValidating, setIsValidating] = useState(false);
  const validating = useRef(false);

  const currentStep = steps[activeStep - 1];
  const isLastStep = activeStep === steps.length;

  const form = useForm<TFormValues>({
    resolver: zodResolver(currentStep.schema as ZodObject<any>) as Resolver<TFormValues>,
    defaultValues,
  });

  const handleStepChange = async (val: number) => {
    if (validating.current || val === activeStep || val < 1 || val > steps.length) return;
    if (val < activeStep) {
      setActiveStep(val);
      return;
    }
    if (val === activeStep + 1) {
      await form.handleSubmit(handleStepForward)();
    }
  };

  const handleStepBack = () => {
    if (validating.current) return;
    if (activeStep === 1) {
      onCancel();
    } else {
      setActiveStep(activeStep - 1);
    }
  };

  const handleStepForward: SubmitHandler<TFormValues> = async () => {
    if (validating.current) return;
    const values = form.getValues();

    if (!isLastStep) {
      validating.current = true;
      setIsValidating(true);
      form.clearErrors("root.server");
      try {
        const error = await currentStep.validateAsync?.(values);
        if (error?.errors.length) {
          applyValidationErrors(form, error);
          return;
        }
        setActiveStep(activeStep + 1);
      } catch {
        form.setError("root.server", {
          type: "custom",
          message: "Kunne ikke validere skjemaet. Prøv igjen.",
        });
      } finally {
        validating.current = false;
        setIsValidating(false);
      }
    } else {
      await onSubmit(values, form);
    }
  };

  return {
    activeStep,
    currentStep,
    isLastStep,
    isValidating,
    form,
    handleStepChange,
    handleStepBack,
    handleStepForward,
  };
}
