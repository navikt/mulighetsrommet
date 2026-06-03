import { type ComboboxProps, UNSAFE_Combobox } from "@navikt/ds-react";
import {
  type FieldPath,
  type FieldValues,
  type RegisterOptions,
  useController,
  useFormContext,
} from "react-hook-form";

export interface ComboboxOption {
  label: string;
  value: string;
}

type FormComboboxProps<TFieldValues extends FieldValues> = Omit<
  ComboboxProps,
  "value" | "error" | "name" | "options" | "selectedOptions" | "isMultiSelect"
> & {
  name: FieldPath<TFieldValues>;
  options: ComboboxOption[];
  rules?: RegisterOptions<TFieldValues>;
};

export function FormCombobox<TFieldValues extends FieldValues>({
  name,
  options,
  rules,
  size = "small",
  onToggleSelected: onToggleSelectedProp,
  ...props
}: FormComboboxProps<TFieldValues>) {
  const { control } = useFormContext<TFieldValues>();
  const { field, fieldState } = useController({ name, control, rules });

  const value = field.value as string | undefined;
  const resolvedOptions = getResolvedOptions(options, value);
  const selectedOptions = resolvedOptions.filter((o) => o.value === value);

  function handleToggleSelected(option: string, isSelected: boolean, isCustomOption: boolean) {
    field.onChange(isSelected ? option : null);
    onToggleSelectedProp?.(option, isSelected, isCustomOption);
  }

  return (
    <UNSAFE_Combobox
      {...props}
      size={size}
      name={field.name}
      options={resolvedOptions}
      selectedOptions={selectedOptions}
      error={fieldState.error?.message}
      onToggleSelected={handleToggleSelected}
    />
  );
}

function getResolvedOptions(options: ComboboxOption[], value: string | undefined) {
  if (typeof value === "string" && value && !options.some((o) => o.value === value)) {
    return [...options, { value: value, label: value }];
  }
  return options;
}
