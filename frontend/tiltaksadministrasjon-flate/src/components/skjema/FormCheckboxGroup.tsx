import { Checkbox, CheckboxGroup, type CheckboxGroupProps } from "@navikt/ds-react";
import {
  type FieldPath,
  type FieldValues,
  type RegisterOptions,
  useController,
  useFormContext,
} from "react-hook-form";
import { ReactNode } from "react";

type Option = { value: string; label: string };

type FormCheckboxGroupProps<TFieldValues extends FieldValues> = Omit<
  CheckboxGroupProps,
  "onChange" | "value" | "children"
> & {
  name: FieldPath<TFieldValues>;
  rules?: RegisterOptions<TFieldValues>;
  options?: Option[];
  children?: ReactNode;
};

export function FormCheckboxGroup<TFieldValues extends FieldValues>({
  name,
  rules,
  ...props
}: FormCheckboxGroupProps<TFieldValues>) {
  const { control } = useFormContext<TFieldValues>();
  const { field, fieldState } = useController({ name, control, rules });

  const children = props.children
    ? props.children
    : props.options?.map((option) => (
        <Checkbox key={option.value} value={option.value}>
          {option.label}
        </Checkbox>
      ));

  return (
    <CheckboxGroup
      {...props}
      value={field.value ?? []}
      onChange={field.onChange}
      error={fieldState.error?.message}
    >
      {children}
    </CheckboxGroup>
  );
}
