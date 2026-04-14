import { Checkbox, type CheckboxProps } from "@navikt/ds-react";
import {
  useController,
  useFormContext,
  type FieldPath,
  type FieldValues,
  type RegisterOptions,
} from "react-hook-form";

type FormCheckboxProps<TFieldValues extends FieldValues> = Omit<
  CheckboxProps,
  "checked" | "onChange" | "name"
> & {
  name: FieldPath<TFieldValues>;
  rules?: RegisterOptions<TFieldValues>;
};

export function FormCheckbox<TFieldValues extends FieldValues>({
  name,
  rules,
  ...props
}: FormCheckboxProps<TFieldValues>) {
  const { control } = useFormContext<TFieldValues>();
  const { field } = useController({ name, control, rules });

  return (
    <Checkbox
      {...props}
      name={field.name}
      ref={field.ref}
      checked={!!field.value}
      onChange={(e) => field.onChange(e.target.checked)}
      onBlur={field.onBlur}
    />
  );
}
