import { TextField, type TextFieldProps } from "@navikt/ds-react";
import {
  type FieldPath,
  type FieldValues,
  type RegisterOptions,
  useController,
  useFormContext,
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
  size = "small",
  ...props
}: FormTextFieldProps<TFieldValues>) {
  const { control } = useFormContext<TFieldValues>();
  const { field, fieldState } = useController({ name, control, rules });

  return (
    <TextField
      {...props}
      {...field}
      size={size}
      value={field.value ?? ""}
      error={fieldState.error?.message}
    />
  );
}
