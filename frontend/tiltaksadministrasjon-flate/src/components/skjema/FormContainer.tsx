import { Separator } from "@mr/frontend-common/components/datadriven/Metadata";
import { ReactNode, SubmitEventHandler } from "react";
import { FieldValues, FormProvider, UseFormReturn } from "react-hook-form";
import { FormButtons } from "@/components/skjema/FormButtons";

interface GenericFormProps<TFormValues extends FieldValues> {
  heading?: string;
  methods: UseFormReturn<TFormValues>;
  onSubmit: SubmitEventHandler<HTMLFormElement>;
  children: ReactNode;
}

export function FormContainer<TFormValues extends FieldValues = FieldValues>({
  heading,
  methods,
  onSubmit,
  children,
}: GenericFormProps<TFormValues>) {
  return (
    <FormProvider {...methods}>
      <form onSubmit={onSubmit}>
        {heading && (
          <>
            <FormButtons heading={heading} />
            <Separator />
          </>
        )}
        {children}
        <Separator />
        <FormButtons />
      </form>
    </FormProvider>
  );
}
