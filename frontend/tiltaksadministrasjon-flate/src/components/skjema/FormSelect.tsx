import { Select, type SelectProps } from "@navikt/ds-react";
import {
  useController,
  useFormContext,
  type FieldPath,
  type FieldValues,
  type RegisterOptions,
} from "react-hook-form";

type FormSelectProps<TFieldValues extends FieldValues> = Omit<
  SelectProps,
  "value" | "onChange" | "error" | "name"
> & {
  name: FieldPath<TFieldValues>;
  rules?: RegisterOptions<TFieldValues>;
};

export function FormSelect<TFieldValues extends FieldValues>({
  name,
  rules,
  size = "small",
  ...props
}: FormSelectProps<TFieldValues>) {
  const { control } = useFormContext<TFieldValues>();
  const { field, fieldState } = useController({ name, control, rules });

  return (
    <Select
      size={size}
      {...props}
      {...field}
      value={field.value ?? ""}
      error={fieldState.error?.message}
    />
  );
}
