import { Checkbox, CheckboxGroup, type CheckboxGroupProps } from "@navikt/ds-react";
import {
  useController,
  useFormContext,
  type FieldPath,
  type FieldValues,
  type RegisterOptions,
} from "react-hook-form";

type Option = { value: string; label: string };

type FormCheckboxGroupProps<TFieldValues extends FieldValues> = Omit<
  CheckboxGroupProps,
  "onChange" | "value" | "children"
> & {
  name: FieldPath<TFieldValues>;
  options: Option[];
  rules?: RegisterOptions<TFieldValues>;
};

export function FormCheckboxGroup<TFieldValues extends FieldValues>({
  name,
  options,
  rules,
  ...props
}: FormCheckboxGroupProps<TFieldValues>) {
  const { control } = useFormContext<TFieldValues>();
  const { field, fieldState } = useController({ name, control, rules });

  return (
    <CheckboxGroup
      {...props}
      value={field.value ?? []}
      onChange={field.onChange}
      error={fieldState.error?.message}
    >
      {options.map((option) => (
        <Checkbox key={option.value} value={option.value}>
          {option.label}
        </Checkbox>
      ))}
    </CheckboxGroup>
  );
}
