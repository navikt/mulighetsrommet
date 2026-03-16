import { Textarea, type TextareaProps } from "@navikt/ds-react";
import {
  useController,
  useFormContext,
  type FieldPath,
  type FieldValues,
  type RegisterOptions,
} from "react-hook-form";

type FormTextareaProps<TFieldValues extends FieldValues> = Omit<
  TextareaProps,
  "value" | "onChange" | "error" | "name"
> & {
  name: FieldPath<TFieldValues>;
  rules?: RegisterOptions<TFieldValues>;
};

export function FormTextarea<TFieldValues extends FieldValues>({
  name,
  rules,
  size = "small",
  ...props
}: FormTextareaProps<TFieldValues>) {
  const { control } = useFormContext<TFieldValues>();
  const { field, fieldState } = useController({ name, control, rules });

  return (
    <Textarea
      size={size}
      {...props}
      {...field}
      value={field.value ?? ""}
      error={fieldState.error?.message}
    />
  );
}
