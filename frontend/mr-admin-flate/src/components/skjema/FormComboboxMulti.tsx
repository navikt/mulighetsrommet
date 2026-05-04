import { type ComboboxProps, UNSAFE_Combobox } from "@navikt/ds-react";
import {
  type FieldPath,
  type FieldValues,
  type RegisterOptions,
  useController,
  useFormContext,
} from "react-hook-form";

type Option = { label: string; value: string };

type FormComboboxMultiProps<TFieldValues extends FieldValues> = Omit<
  ComboboxProps,
  "value" | "error" | "name" | "options" | "selectedOptions" | "isMultiSelect"
> & {
  name: FieldPath<TFieldValues>;
  options: Option[];
  rules?: RegisterOptions<TFieldValues>;
};

export function FormComboboxMulti<TFieldValues extends FieldValues>({
  name,
  options,
  rules,
  size = "small",
  onToggleSelected: onToggleSelectedProp,
  ...props
}: FormComboboxMultiProps<TFieldValues>) {
  const { control } = useFormContext<TFieldValues>();
  const { field, fieldState } = useController({ name, control, rules });

  const values = Array.isArray(field.value) ? (field.value as string[]) : [];
  const resolvedOptions = getResolvedOptions(options, values);
  const selectedOptions = resolvedOptions.filter((o) => values.includes(o.value));

  function handleToggleSelected(option: string, isSelected: boolean, isCustomOption: boolean) {
    const selectedValues = isSelected ? [...values, option] : values.filter((v) => v !== option);
    field.onChange(selectedValues);
    onToggleSelectedProp?.(option, isSelected, isCustomOption);
  }

  return (
    <UNSAFE_Combobox
      {...props}
      size={size}
      isMultiSelect
      name={field.name}
      options={resolvedOptions}
      selectedOptions={selectedOptions}
      error={fieldState.error?.message}
      onToggleSelected={handleToggleSelected}
    />
  );
}

function getResolvedOptions(options: Option[], values: string[]) {
  const missing = values.filter((value) => !options.some((o) => o.value === value));
  return missing.length > 0
    ? [...options, ...missing.map((value) => ({ value, label: value }))]
    : options;
}
