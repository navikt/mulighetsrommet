import { TextField, VStack, type TextFieldProps } from "@navikt/ds-react";
import {
  useController,
  useFormContext,
  type FieldPath,
  type FieldValues,
  type RegisterOptions,
} from "react-hook-form";

type FormTextFieldProps<TFieldValues extends FieldValues> = Omit<
  TextFieldProps,
  "value" | "onChange" | "error" | "name"
> & {
  name: FieldPath<TFieldValues>;
  rules?: RegisterOptions<TFieldValues>;
};

export function FormTextField<TFieldValues extends FieldValues>({
  name,
  rules,
  ...props
}: FormTextFieldProps<TFieldValues>) {
  const { control } = useFormContext<TFieldValues>();
  const { field, fieldState } = useController({ name, control, rules });

  return (
    <VStack align="start">
      <TextField
        {...props}
        {...field}
        value={field.value ?? ""}
        error={fieldState.error?.message}
      />
    </VStack>
  );
}
