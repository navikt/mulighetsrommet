import { type ComboboxProps, UNSAFE_Combobox } from "@navikt/ds-react";
import {
  type FieldPath,
  type FieldValues,
  type RegisterOptions,
  useController,
  useFormContext,
} from "react-hook-form";

type Option = { label: string; value: string };

type FormComboboxProps<TFieldValues extends FieldValues> = Omit<
  ComboboxProps,
  "value" | "onChange" | "error" | "name" | "options" | "selectedOptions" | "onToggleSelected"
> & {
  name: FieldPath<TFieldValues>;
  options: Option[];
  rules?: RegisterOptions<TFieldValues>;
};

export function FormCombobox<TFieldValues extends FieldValues>({
  name,
  options,
  rules,
  size = "small",
  ...props
}: FormComboboxProps<TFieldValues>) {
  const { control } = useFormContext<TFieldValues>();
  const { field, fieldState } = useController({ name, control, rules });

  // Inkluder gjeldende verdi som et alternativ selv om den ikke finnes i options-listen
  // slik at valgte verdier alltid vises i comboboxen
  const resolvedOptions: Option[] =
    field.value && !options.some((o) => o.value === field.value)
      ? [...options, { value: field.value, label: field.value }]
      : options;

  const selectedOptions = resolvedOptions.filter((o) => o.value === field.value);

  return (
    <UNSAFE_Combobox
      {...props}
      size={size}
      name={field.name}
      options={resolvedOptions}
      selectedOptions={selectedOptions}
      error={fieldState.error?.message}
      onToggleSelected={(value, isSelected) => {
        field.onChange(isSelected ? value : null);
      }}
    />
  );
}
